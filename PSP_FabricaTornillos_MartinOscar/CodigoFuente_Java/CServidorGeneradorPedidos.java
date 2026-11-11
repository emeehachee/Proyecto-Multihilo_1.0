/*
 * ==============================================================================
 * PROYECTO: SIMULADOR INDUSTRIAL - FABRICA DE TORNILLOS
 * CLASE   : CServidorGeneradorPedidos.java
 * AUTOR   : David Coello (Adaptado para 2º DAM - PSP)
 * FECHA   : Octubre 2026
 * MODULO  : Programacion de Servicios y Procesos (Java 21 LTS)
 * 
 * DESCRIPCION:
 * Servidor generador de pedidos industriales concurrentes. Simula la llegada
 * estocastica de pedidos desde 6 proveedores externos independientes, cada uno
 * con su propio directorio de entrada y su propia sintaxis de documento comercial.
 * 
 * CARACTERISTICAS:
 * 1. Nomenclatura Hungara rigurosa en tipos, identificadores y estructuras.
 * 2. Fichero de configuracion minimo y limpio ('servidor_pedidos.conf'):
 *    - Frecuencia media de pedidos por minuto (retardos aleatorios dentro del ciclo).
 *    - Ruta base de ejecucion.
 *    - Modo test para desarrollo.
 *    - Pesos asignados a cada uno de los 6 proveedores (peso_proveedor_X).
 * 3. Los datos corporativos (CIF, Razon Social, formato) estan fijados internamente
 *    para evitar que configuraciones erroneas rompan los layouts de texto.
 * 4. Creacion automatica de directorios de entrada (proveedor1 a proveedor6).
 * 5. Escritura atomica de ficheros (.tmp -> .txt) para evitar lecturas concurrentes
 *    incompletas por parte de los lectores.
 * 6. Control exhaustivo de excepciones y apagado limpio con informe (ShutdownHook).
 * 
 * FORMATOS DE PEDIDO (1 TIPO DE TORNILLO POR PEDIDO):
 * - Proveedor 1: Automocion del Cantabrico S.A. -> CSV ERP con metadatos (#) y ';'
 * - Proveedor 2: Ferreteria Industrial del Duero S.L. -> Albaran ASCII preformateado
 * - Proveedor 3: Construcciones Metalicas Ibericas S.A. -> Posicional Ancho Fijo
 *                (Norma 19 / CSB 19 Bancaria Espanola, 80 columnas exactas)
 * - Proveedor 4: Suministros Navales del Norte S.A. -> Segmentos EDI/ODETTE ('|')
 * - Proveedor 5: Electro-Mecanica Levante C.B. -> Propiedades B2B [SECCION] clave=valor
 * - Proveedor 6: Robotica y Ensamble Automatizado S.L. -> Sintaxis SCADA / DSL
 * ==============================================================================
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class CServidorGeneradorPedidos {

    // --------------------------------------------------------------------------
    // CONSTANTES GENERALES DEL SISTEMA
    // --------------------------------------------------------------------------
    private static final String sNOMBRE_FICHERO_CONF_DEFECTO = "servidor_pedidos.conf";
    private static final String[] aTIPOS_TORNILLOS = { "T1", "T2", "T3" };
    private static final String[] aFORMATOS_EMPAQUE = { "D", "P", "C" }; // D=12, P=2, C=100

    // Metadatos internos fijos de los 6 proveedores (CIFs oficiales y Nombres)
    private static final String[] aNOMBRES_EMPRESAS = {
        "Automocion del Cantabrico S.A.",
        "Ferreteria Industrial del Duero S.L.",
        "Construcciones Metalicas Ibericas S.A.",
        "Suministros Navales del Norte S.A.",
        "Electro-Mecanica Levante C.B.",
        "Robotica y Ensamble Automatizado S.L."
    };

    private static final String[] aCIFS_EMPRESAS = {
        "A33012948", // Prov 1
        "B47120934", // Prov 2
        "A28901234", // Prov 3 (9 car. fijos para Norma 19)
        "A39871120", // Prov 4
        "E46991823", // Prov 5
        "B84551209"  // Prov 6
    };

    // --------------------------------------------------------------------------
    // ATRIBUTOS DEL SERVIDOR (NOMENCLATURA HUNGARA)
    // --------------------------------------------------------------------------
    private final CConfiguracion oConfiguracion;
    private final Random oRandom;
    private volatile boolean bEjecutando;
    private int iContadorGlobalPedidos;

    // Estadisticas acumuladas para el balance final
    private final int[] aEstadisticasProveedores;
    private final int[] aEstadisticasTiposTornillos; // 0=T1, 1=T2, 2=T3
    private final int[] aEstadisticasFormatos;       // 0=D, 1=P, 2=C

    /**
     * Constructor principal del servidor generador.
     * 
     * @param sRutaConfiguracion Ruta al fichero de configuracion .conf
     */
    public CServidorGeneradorPedidos(String sRutaConfiguracion) {
        this.oRandom = new Random();
        this.bEjecutando = false;
        this.iContadorGlobalPedidos = 0;
        this.aEstadisticasProveedores = new int[6];
        this.aEstadisticasTiposTornillos = new int[3];
        this.aEstadisticasFormatos = new int[3];

        // Carga de la configuracion con control de fallos
        this.oConfiguracion = new CConfiguracion(sRutaConfiguracion);
    }

    /**
     * Metodo principal de ejecucion del servidor de pedidos.
     */
    public void iniciarServidor() {
        this.bEjecutando = true;

        System.out.println("===============================================================================");
        System.out.println("  SERVIDOR GENERADOR DE PEDIDOS INDUSTRIALES (SIMULACION 2º DAM)");
        System.out.println("===============================================================================");
        System.out.println("Ruta Base de Trabajo        : " + oConfiguracion.obtenerRutaBase());
        System.out.println("Frecuencia Configurada      : " + oConfiguracion.obtenerFrecuenciaPorMinuto() + " pedidos/min");
        System.out.println("Modo Test (Desarrollo Rapido): " + (oConfiguracion.esModoTest() ? "ACTIVADO" : "DESACTIVADO"));
        System.out.println("-------------------------------------------------------------------------------");

        // 1. Verificacion y creacion automatica de los directorios de entrada
        asegurarDirectoriosProveedores();

        // 2. Registro del hook de apagado para Ctrl+C (parada limpia y estadisticas)
        registrarHookApagado();

        System.out.println("[INFO] Servidor activo. Presione Ctrl+C en cualquier momento para detener.");
        System.out.println("-------------------------------------------------------------------------------");

        // 3. Bucle principal de generacion continua
        while (bEjecutando) {
            try {
                // Calcular retardo estocastico dentro del ciclo de generacion
                int iRetardoMs = calcularRetardoAleatorioMs();

                // Esperar el intervalo calculado
                Thread.sleep(iRetardoMs);

                if (!bEjecutando) {
                    break;
                }

                // Seleccionar proveedor mediante ruleta ponderada
                CProveedor oProveedorSeleccionado = seleccionarProveedorPonderado();

                // Generar datos aleatorios del pedido (1 tipo de tornillo)
                iContadorGlobalPedidos++;
                CPedido oPedido = generarDatosPedido(oProveedorSeleccionado, iContadorGlobalPedidos);

                // Crear el archivo en el formato especifico de dicho proveedor
                escribirArchivoPedidoAtomico(oProveedorSeleccionado, oPedido);

                // Actualizar metricas acumuladas
                actualizarEstadisticas(oProveedorSeleccionado, oPedido);

            } catch (InterruptedException oInterruptedException) {
                // Restaurar el estado de interrupcion y salir limpiamente
                Thread.currentThread().interrupt();
                bEjecutando = false;
                System.out.println("[AVISO] Bucle de generacion interrumpido.");
            } catch (Exception oExcepcionGeneral) {
                System.err.println("[ERROR] Error imprevisto en ciclo de generacion: " + oExcepcionGeneral.getMessage());
            }
        }
    }

    /**
     * Asegura que el directorio base y los 6 subdirectorios de proveedores existan.
     * Si no existen, los crea dinamicamente en disco.
     */
    private void asegurarDirectoriosProveedores() {
        String sRutaBase = oConfiguracion.obtenerRutaBase();
        File oDirBase = new File(sRutaBase);

        if (!oDirBase.exists()) {
            boolean bCreado = oDirBase.mkdirs();
            System.out.println("[SISTEMA] Creando directorio base: " + oDirBase.getAbsolutePath() + " -> " + (bCreado ? "OK" : "ERROR"));
        }

        List<CProveedor> aProveedores = oConfiguracion.obtenerListaProveedores();
        for (CProveedor oProv : aProveedores) {
            File oDirProveedor = new File(oDirBase, oProv.obtenerDirectorioRelativo());
            if (!oDirProveedor.exists()) {
                boolean bCreado = oDirProveedor.mkdirs();
                System.out.println("[SISTEMA] Creando buzon de entrada [" + oProv.obtenerIdentificador() + "]: " 
                        + oDirProveedor.getPath() + " -> " + (bCreado ? "OK" : "ERROR"));
            } else {
                System.out.println("[SISTEMA] Buzon existente [" + oProv.obtenerIdentificador() + "]: " + oDirProveedor.getPath());
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * Calcula el retardo en milisegundos para el siguiente pedido.
     * Distribuye los pedidos de forma aleatoria respetando la media por minuto.
     * 
     * @return Retardo en milisegundos
     */
    private int calcularRetardoAleatorioMs() {
        if (oConfiguracion.esModoTest()) {
            // Modo test: retardos reducidos entre 100 y 500 ms para pruebas agiles
            return 100 + oRandom.nextInt(400);
        }

        int iFrecuenciaPorMinuto = oConfiguracion.obtenerFrecuenciaPorMinuto();
        if (iFrecuenciaPorMinuto <= 0) {
            iFrecuenciaPorMinuto = 12; // Salvaguarda
        }

        // Intervalo medio teorico = 60.000 ms / frecuencia
        double dIntervaloMedioMs = 60000.0 / (double) iFrecuenciaPorMinuto;

        // Variacion aleatoria uniforme entre 35% y 165% de la media
        double dFactor = 0.35 + (oRandom.nextDouble() * 1.30);
        int iRetardoCalculado = (int) (dIntervaloMedioMs * dFactor);

        // Limites de pliego de la practica (1.000 ms a 10.000 ms entre llegadas)
        if (iRetardoCalculado < 1000) {
            iRetardoCalculado = 1000;
        } else if (iRetardoCalculado > 10000) {
            iRetardoCalculado = 10000;
        }

        return iRetardoCalculado;
    }

    /**
     * Selecciona un proveedor utilizando el algoritmo de ruleta ponderada segun pesos.
     * 
     * @return Instancia del proveedor seleccionado
     */
    private CProveedor seleccionarProveedorPonderado() {
        List<CProveedor> aProveedores = oConfiguracion.obtenerListaProveedores();
        int iSumaPesosTotal = 0;
        for (CProveedor oProv : aProveedores) {
            iSumaPesosTotal += oProv.obtenerPeso();
        }

        if (iSumaPesosTotal <= 0) {
            return aProveedores.get(0); // Salvaguarda
        }

        int iTiradaAleatoria = oRandom.nextInt(iSumaPesosTotal) + 1; // 1 .. iSumaPesosTotal
        int iAcumulado = 0;

        for (CProveedor oProv : aProveedores) {
            iAcumulado += oProv.obtenerPeso();
            if (iTiradaAleatoria <= iAcumulado) {
                return oProv;
            }
        }

        return aProveedores.get(aProveedores.size() - 1);
    }

    /**
     * Genera datos coherentes para un nuevo pedido (1 solo tipo de tornillo).
     * 
     * @param oProveedor Proveedor emisor
     * @param iNumeroSecuencial Numero consecutivo de pedido
     * @return Objeto CPedido poblado con datos
     */
    private CPedido generarDatosPedido(CProveedor oProveedor, int iNumeroSecuencial) {
        // Seleccion de tipo de tornillo aleatorio
        String sTipoTornillo = aTIPOS_TORNILLOS[oRandom.nextInt(aTIPOS_TORNILLOS.length)];

        // Seleccion de formato de embalaje aleatorio (D=12, P=2, C=100)
        String sFormatoEmpaque = aFORMATOS_EMPAQUE[oRandom.nextInt(aFORMATOS_EMPAQUE.length)];

        // Generar cantidades acordes al tipo de empaque (incluyendo sobrantes para probar logica)
        int iCantidad;
        if ("P".equals(sFormatoEmpaque)) {
            // Pares (2): cantidades entre 10 y 60 (a veces impares para probar unidad sobrante)
            iCantidad = 10 + oRandom.nextInt(51);
        } else if ("D".equals(sFormatoEmpaque)) {
            // Docenas (12): cantidades entre 24 y 180 (con sobrantes frecuentes)
            int iDocenasAprox = 2 + oRandom.nextInt(12);
            int iSobrante = oRandom.nextInt(12);
            iCantidad = (iDocenasAprox * 12) + iSobrante;
        } else {
            // Centenas (100): cantidades entre 100 y 600
            int iCentenasAprox = 1 + oRandom.nextInt(5);
            int iSobrante = (oRandom.nextInt(10) == 0) ? oRandom.nextInt(100) : 0;
            iCantidad = (iCentenasAprox * 100) + iSobrante;
        }

        // Construccion del identificador corporativo
        String sIdPedidoPropio = construirIdPedidoPropio(oProveedor.obtenerIdentificador(), iNumeroSecuencial);

        return new CPedido(sIdPedidoPropio, sTipoTornillo, iCantidad, sFormatoEmpaque, new Date());
    }

    /**
     * Construye un identificador corporativo de pedido personalizado por empresa.
     */
    private String construirIdPedidoPropio(int iIdProveedor, int iSecuencial) {
        String sAnioActual = new SimpleDateFormat("yyyy").format(new Date());
        return switch (iIdProveedor) {
            case 1 -> String.format("AC-%s-%04d", sAnioActual, iSecuencial);
            case 2 -> String.format("FID-%s-%04d", sAnioActual, iSecuencial);
            case 3 -> String.format("CMI-%s%04d", sAnioActual, iSecuencial); // 12 car. fijo para Norma 19
            case 4 -> String.format("SNN-%s-%04d", sAnioActual, iSecuencial);
            case 5 -> String.format("EML-%s-%04d", sAnioActual, iSecuencial);
            case 6 -> String.format("REA-%s-%04d", sAnioActual, iSecuencial);
            default -> String.format("PED-%s-%04d", sAnioActual, iSecuencial);
        };
    }

    /**
     * Escribe de forma atomica el pedido en el buzon del proveedor:
     * Primero se escribe en un archivo temporal (.tmp) y luego se renombra atómicamente a (.txt).
     * Esto evita que el coordinador lea un archivo a medio escribir.
     */
    private void escribirArchivoPedidoAtomico(CProveedor oProveedor, CPedido oPedido) {
        String sRutaBase = oConfiguracion.obtenerRutaBase();
        File oDirProveedor = new File(sRutaBase, oProveedor.obtenerDirectorioRelativo());

        String sNombreFicheroFinal = String.format("pedido_%s.txt", oPedido.obtenerIdPedido());
        String sNombreFicheroTmp = String.format("pedido_%s.tmp", oPedido.obtenerIdPedido());

        File oFicheroTmp = new File(oDirProveedor, sNombreFicheroTmp);
        File oFicheroFinal = new File(oDirProveedor, sNombreFicheroFinal);

        // Generar el contenido en el formato sintactico particular del proveedor
        String sContenido = CGeneradorFormatos.generarTextoPedido(oProveedor, oPedido);

        try {
            // Escritura en fichero temporal con codificacion UTF-8
            try (BufferedWriter oWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(oFicheroTmp), StandardCharsets.UTF_8))) {
                oWriter.write(sContenido);
                oWriter.flush();
            }

            // Movimiento / renombramiento atomico a .txt
            Path oPathTmp = oFicheroTmp.toPath();
            Path oPathFinal = oFicheroFinal.toPath();
            Files.move(oPathTmp, oPathFinal, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            System.out.printf("[GENERADO] Prov #%d (%s) -> %s | Tornillo: %s | Cant: %4d | Pack: %s%n",
                    oProveedor.obtenerIdentificador(),
                    oProveedor.obtenerNombreCorto(),
                    oFicheroFinal.getName(),
                    oPedido.obtenerTipoTornillo(),
                    oPedido.obtenerCantidad(),
                    oPedido.obtenerFormatoEmpaque());

        } catch (IOException oIOException) {
            System.err.println("[ERROR E/S] No se pudo escribir pedido en " + oFicheroFinal.getAbsolutePath() 
                    + ": " + oIOException.getMessage());
            if (oFicheroTmp.exists() && !oFicheroTmp.delete()) {
                System.err.println("[AVISO] No se pudo eliminar el fichero temporal huerfano: " + oFicheroTmp.getName());
            }
        }
    }

    /**
     * Actualiza contadores para el balance al finalizar la simulacion.
     */
    private void actualizarEstadisticas(CProveedor oProveedor, CPedido oPedido) {
        int iIdxProv = oProveedor.obtenerIdentificador() - 1;
        if (iIdxProv >= 0 && iIdxProv < aEstadisticasProveedores.length) {
            aEstadisticasProveedores[iIdxProv]++;
        }

        // Tipo tornillo
        switch (oPedido.obtenerTipoTornillo()) {
            case "T1" -> aEstadisticasTiposTornillos[0]++;
            case "T2" -> aEstadisticasTiposTornillos[1]++;
            case "T3" -> aEstadisticasTiposTornillos[2]++;
        }

        // Formato empaque
        switch (oPedido.obtenerFormatoEmpaque()) {
            case "D" -> aEstadisticasFormatos[0]++;
            case "P" -> aEstadisticasFormatos[1]++;
            case "C" -> aEstadisticasFormatos[2]++;
        }
    }

    /**
     * Registra un gancho de parada de la JVM para emitir el balance al pulsar Ctrl+C.
     */
    private void registrarHookApagado() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bEjecutando = false;
            System.out.println("\n");
            System.out.println("===============================================================================");
            System.out.println("             INFORME FINAL DE SIMULACION - SERVIDOR DE PEDIDOS                 ");
            System.out.println("===============================================================================");
            System.out.printf("Total Pedidos Generados  : %d%n", iContadorGlobalPedidos);
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println("Distribucion por Proveedor (Validacion de Pesos):");
            List<CProveedor> aProveedores = oConfiguracion.obtenerListaProveedores();
            for (CProveedor oProv : aProveedores) {
                int iIdx = oProv.obtenerIdentificador() - 1;
                int iGenerados = (iIdx >= 0 && iIdx < aEstadisticasProveedores.length) ? aEstadisticasProveedores[iIdx] : 0;
                double dPorcentaje = (iContadorGlobalPedidos > 0) ? (iGenerados * 100.0 / iContadorGlobalPedidos) : 0.0;
                System.out.printf("  - Prov %d [%-32s] (Peso %2d): %4d pedidos (%.1f%%)%n",
                        oProv.obtenerIdentificador(),
                        oProv.obtenerNombreCorto(),
                        oProv.obtenerPeso(),
                        iGenerados,
                        dPorcentaje);
            }
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println("Distribucion por Tipo de Tornillo:");
            System.out.printf("  - T1: %d | T2: %d | T3: %d%n",
                    aEstadisticasTiposTornillos[0], aEstadisticasTiposTornillos[1], aEstadisticasTiposTornillos[2]);
            System.out.println("Distribucion por Formato de Empaque:");
            System.out.printf("  - D (Docenas/12): %d | P (Pares/2): %d | C (Centenas/100): %d%n",
                    aEstadisticasFormatos[0], aEstadisticasFormatos[1], aEstadisticasFormatos[2]);
            System.out.println("===============================================================================");
            System.out.println("[FIN] Servidor detenido de forma limpia y ordenada.");
        }));
    }

    // ==========================================================================
    // METODO MAIN: PUNTO DE ENTRADA DE LA APLICACION
    // ==========================================================================
    public static void main(String[] args) {
         System.out.println("Directorio de trabajo: " + System.getProperty("user.dir"));
        String sRutaConfiguracion = (args.length > 0) ? args[0] : sNOMBRE_FICHERO_CONF_DEFECTO;
        CServidorGeneradorPedidos oServidor = new CServidorGeneradorPedidos(sRutaConfiguracion);
        oServidor.iniciarServidor();
    }

    // ==========================================================================
    // CLASE AUXILIAR: CONFIGURACION MINIMA Y LIMPIA (CConfiguracion)
    // ==========================================================================
    public static class CConfiguracion {
        private int iFrecuenciaPorMinuto;
        private String sRutaBase;
        private boolean bModoTest;
        private final List<CProveedor> aProveedores;

        public CConfiguracion(String sRutaFicheroConf) {
            this.aProveedores = new ArrayList<>();
            cargarConfiguracion(sRutaFicheroConf);
        }

        private void cargarConfiguracion(String sRutaFicheroConf) {
            File oFicheroConf = new File(sRutaFicheroConf);
            Properties oPropiedades = new Properties();

            if (!oFicheroConf.exists()) {
                System.out.println("[AVISO] No se encontro '" + sRutaFicheroConf + "'. Creando configuracion por defecto...");
                crearConfiguracionDefecto(oFicheroConf);
            }

            try (FileInputStream oInput = new FileInputStream(oFicheroConf);
                 InputStreamReader oReader = new InputStreamReader(oInput, StandardCharsets.UTF_8)) {
                oPropiedades.load(oReader);
            } catch (IOException oIOException) {
                System.err.println("[ERROR] No se pudo leer '" + sRutaFicheroConf + "': " + oIOException.getMessage());
            }

            // Lectura de parametros operativos limpios
            this.iFrecuenciaPorMinuto = obtenerEntero(oPropiedades, "frecuencia_pedidos_por_minuto", 12);
            this.sRutaBase = oPropiedades.getProperty("ruta_base", ".").trim();
            this.bModoTest = Boolean.parseBoolean(oPropiedades.getProperty("modo_test", "false").trim());

            // Pesos por defecto si no estan especificados
            int[] aPesosDefecto = { 25, 20, 15, 15, 15, 10 };

            // Inicializar los 6 proveedores con sus metadatos fijos y pesos del conf
            aProveedores.clear();
            for (int i = 1; i <= 6; i++) {
                // Acepta "peso_proveedor_1" o alternativamente "proveedor.1.peso"
                int iPeso = obtenerEntero(oPropiedades, "peso_proveedor_" + i, -1);
                if (iPeso == -1) {
                    iPeso = obtenerEntero(oPropiedades, "proveedor." + i + ".peso", aPesosDefecto[i - 1]);
                }

                String sNombre = aNOMBRES_EMPRESAS[i - 1];
                String sCif = aCIFS_EMPRESAS[i - 1];
                String sDir = "proveedor" + i;

                aProveedores.add(new CProveedor(i, sNombre, sCif, sDir, iPeso));
            }
        }

        private int obtenerEntero(Properties oProps, String sClave, int iDefecto) {
            String sValor = oProps.getProperty(sClave);
            if (sValor != null) {
                try {
                    return Integer.parseInt(sValor.trim());
                } catch (NumberFormatException oNFE) {
                    System.err.println("[AVISO] Valor no numerico para '" + sClave + "'. Usando por defecto: " + iDefecto);
                }
            }
            return iDefecto;
        }

        private void crearConfiguracionDefecto(File oFichero) {
            String sContenido = """
                    # ==============================================================================
                    # CONFIGURACION DEL SERVIDOR GENERADOR DE PEDIDOS - SIMULACION INDUSTRIAL
                    # Modulo: Programacion de Servicios y Procesos (2º DAM)
                    # ==============================================================================

                    # Frecuencia global media de pedidos por minuto
                    # (Dentro del minuto los intervalos entre pedidos seran aleatorios)
                    frecuencia_pedidos_por_minuto=12

                    # Directorio base donde se crearan las carpetas de los proveedores
                    # '.' indica la carpeta donde se ejecuta la aplicacion
                    ruta_base=PSP_FabricaTornillos_MartinOscar

                    # Modo test para pruebas rapidas de desarrollo (true = 100 a 500 ms por pedido)
                    modo_test=false

                    # Pesos de probabilidad para cada proveedor (a mayor peso, mas pedidos genera)
                    peso_proveedor_1=25
                    peso_proveedor_2=20
                    peso_proveedor_3=15
                    peso_proveedor_4=15
                    peso_proveedor_5=15
                    peso_proveedor_6=10
                    """;
            try (BufferedWriter oWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(oFichero), StandardCharsets.UTF_8))) {
                oWriter.write(sContenido);
            } catch (IOException oE) {
                System.err.println("[ERROR] No se pudo escribir fichero de configuracion por defecto: " + oE.getMessage());
            }
        }

        public int obtenerFrecuenciaPorMinuto() { return iFrecuenciaPorMinuto; }
        public String obtenerRutaBase() { return sRutaBase; }
        public boolean esModoTest() { return bModoTest; }
        public List<CProveedor> obtenerListaProveedores() { return aProveedores; }
    }

    // ==========================================================================
    // CLASE AUXILIAR: PROVEEDOR EXTERNO (CProveedor)
    // ==========================================================================
    public static class CProveedor {
        private final int iIdentificador;
        private final String sNombre;
        private final String sCif;
        private final String sDirectorioRelativo;
        private final int iPeso;

        public CProveedor(int iIdentificador, String sNombre, String sCif, String sDirectorioRelativo, int iPeso) {
            this.iIdentificador = iIdentificador;
            this.sNombre = sNombre;
            this.sCif = sCif;
            this.sDirectorioRelativo = sDirectorioRelativo;
            this.iPeso = iPeso;
        }

        public int obtenerIdentificador() { return iIdentificador; }
        public String obtenerNombre() { return sNombre; }
        public String obtenerNombreCorto() {
            return sNombre.length() > 28 ? sNombre.substring(0, 25) + "..." : sNombre;
        }
        public String obtenerCif() { return sCif; }
        public String obtenerDirectorioRelativo() { return sDirectorioRelativo; }
        public int obtenerPeso() { return iPeso; }
    }

    // ==========================================================================
    // CLASE AUXILIAR: ESTRUCTURA INTERNA DE UN PEDIDO (CPedido)
    // ==========================================================================
    public static class CPedido {
        private final String sIdPedido;
        private final String sTipoTornillo;
        private final int iCantidad;
        private final String sFormatoEmpaque;
        private final Date oFechaEmision;

        public CPedido(String sIdPedido, String sTipoTornillo, int iCantidad, String sFormatoEmpaque, Date oFechaEmision) {
            this.sIdPedido = sIdPedido;
            this.sTipoTornillo = sTipoTornillo;
            this.iCantidad = iCantidad;
            this.sFormatoEmpaque = sFormatoEmpaque;
            this.oFechaEmision = oFechaEmision;
        }

        public String obtenerIdPedido() { return sIdPedido; }
        public String obtenerTipoTornillo() { return sTipoTornillo; }
        public int obtenerCantidad() { return iCantidad; }
        public String obtenerFormatoEmpaque() { return sFormatoEmpaque; }
        public Date obtenerFechaEmision() { return oFechaEmision; }
    }

    // ==========================================================================
    // GENERADOR DE LOS 6 FORMATOS ESPECIFICOS DE PEDIDO
    // ==========================================================================
    public static class CGeneradorFormatos {

        /**
         * Enruta la generacion del texto segun el proveedor asignado.
         */
        public static String generarTextoPedido(CProveedor oProveedor, CPedido oPedido) {
            return switch (oProveedor.obtenerIdentificador()) {
                case 1 -> generarFormato1_CsvErp(oProveedor, oPedido);
                case 2 -> generarFormato2_AlbaranAscii(oProveedor, oPedido);
                case 3 -> generarFormato3_PosicionalNorma19(oProveedor, oPedido);
                case 4 -> generarFormato4_EdiSegmentos(oProveedor, oPedido);
                case 5 -> generarFormato5_PropiedadesB2B(oProveedor, oPedido);
                case 6 -> generarFormato6_ScadaComandos(oProveedor, oPedido);
                default -> generarFormato1_CsvErp(oProveedor, oPedido);
            };
        }

        // ----------------------------------------------------------------------
        // FORMATO 1: CSV ERP con metadatos (#) y separador ';'
        // Proveedor: Automocion del Cantabrico S.A.
        // ----------------------------------------------------------------------
        private static String generarFormato1_CsvErp(CProveedor oProv, CPedido oPed) {
            SimpleDateFormat oFmtFecha = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat oFmtHora = new SimpleDateFormat("HH:mm:ss");
            String sFecha = oFmtFecha.format(oPed.obtenerFechaEmision());
            String sHora = oFmtHora.format(oPed.obtenerFechaEmision());

            return String.format("""
                    # ==============================================================================
                    # EMPRESA_EMISORA: %s
                    # CIF_EMISOR: %s
                    # FECHA_ORDEN: %s
                    # HORA_ORDEN: %s
                    # CENTRO_ENTREGA: PLANTA GIJON ENSAMBLAJE
                    # ==============================================================================
                    LINEA;COD_PEDIDO;FECHA;TIPO_TORNILLO;CANTIDAD;FORMATO_EMPAQUE;REF_INTERNA;PRECIO_UNITARIO
                    1;%s;%s;%s;%d;%s;REF-PERNO-%s;0.18
                    """,
                    oProv.obtenerNombre(),
                    oProv.obtenerCif(),
                    sFecha,
                    sHora,
                    oPed.obtenerIdPedido(),
                    sFecha,
                    oPed.obtenerTipoTornillo(),
                    oPed.obtenerCantidad(),
                    oPed.obtenerFormatoEmpaque(),
                    oPed.obtenerTipoTornillo()
            );
        }

        // ----------------------------------------------------------------------
        // FORMATO 2: Albaran ASCII Preformateado (Terminal de facturacion / Almacen)
        // Proveedor: Ferreteria Industrial del Duero S.L.
        // ----------------------------------------------------------------------
        private static String generarFormato2_AlbaranAscii(CProveedor oProv, CPedido oPed) {
            SimpleDateFormat oFmtFecha = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat oFmtHora = new SimpleDateFormat("HH:mm:ss");
            String sFecha = oFmtFecha.format(oPed.obtenerFechaEmision());
            String sHora = oFmtHora.format(oPed.obtenerFechaEmision());

            return String.format("""
                    ======================================================================
                    ORDEN DE SUMINISTRO INDUSTRIAL - %s
                    ======================================================================
                    NIF CLIENTE  : %s
                    PEDIDO NUM   : %s
                    FECHA PEDIDO : %s
                    HORA PEDIDO  : %s
                    DESTINO      : NAVE INDUSTRIAL VALLADOLID
                    ----------------------------------------------------------------------
                    POS  REFERENCIA          TIPO  CANTIDAD   EMBALAJE   PLAZO_DIAS
                    ----------------------------------------------------------------------
                    01   TORN-HEX-ALTA-RES   %-4s  %-10d %-10s 03
                    ======================================================================
                    ESTADO: APROBADO | CONTROL_CALIDAD: SI
                    """,
                    oProv.obtenerNombre().toUpperCase(),
                    oProv.obtenerCif(),
                    oPed.obtenerIdPedido(),
                    sFecha,
                    sHora,
                    oPed.obtenerTipoTornillo(),
                    oPed.obtenerCantidad(),
                    oPed.obtenerFormatoEmpaque()
            );
        }

        // ----------------------------------------------------------------------
        // FORMATO 3: Posicional Ancho Fijo Estilo Norma 19 (CSB 19 Bancaria Espanola)
        // Proveedor: Construcciones Metalicas Ibericas S.A.
        // Reglas estrictas: Exactamente 2 lineas de 80 caracteres.
        // Linea 1 (Registro 01): Cabecera ordenante con CIF, Num Pedido, Fecha AAAAMMDD
        // Linea 2 (Registro 02): Detalle de pedido con Num Pedido, Tipo, Cantidad (ceros izq), Empaque
        // ----------------------------------------------------------------------
        private static String generarFormato3_PosicionalNorma19(CProveedor oProv, CPedido oPed) {
            SimpleDateFormat oFmtFecha = new SimpleDateFormat("yyyyMMdd");
            String sFechaAaaaMmDd = oFmtFecha.format(oPed.obtenerFechaEmision());

            // --- REGISTRO 01 (Cabecera Ordenante - 80 caracteres exactos) ---
            // 01-02 (02 car): "01" (Codigo de registro cabecera)
            // 03-11 (09 car): CIF de la empresa emisor
            // 12-23 (12 car): ID Pedido vinculado (alineado izquierda)
            // 24-31 (08 car): Fecha emision AAAAMMDD
            // 32-60 (29 car): Razon social emisor (alineada izquierda)
            // 61-80 (20 car): Centro / Destino planta (alineada izquierda)
            String sCodReg1 = "01";
            String sCifFijo = CUtilidadesCadena.rellenarDerecha(oProv.obtenerCif(), 9);
            String sIdPedFijo1 = CUtilidadesCadena.rellenarDerecha(oPed.obtenerIdPedido(), 12);
            String sRazonSocialFija = CUtilidadesCadena.rellenarDerecha(oProv.obtenerNombre(), 29);
            String sCentroPlantaFija = CUtilidadesCadena.rellenarDerecha("PLANTA MADRID SUR", 20);

            String sRegistro01 = sCodReg1 + sCifFijo + sIdPedFijo1 + sFechaAaaaMmDd + sRazonSocialFija + sCentroPlantaFija;

            // --- REGISTRO 02 (Detalle del Pedido - 80 caracteres exactos) ---
            // 01-02 (02 car): "02" (Codigo de registro detalle)
            // 03-14 (12 car): ID Pedido vinculante
            // 15-16 (02 car): Tipo de tornillo ("T1", "T2", "T3")
            // 17-22 (06 car): Cantidad formateada con ceros a la izquierda ("000120")
            // 23-23 (01 car): Formato de empaque ("D", "P", "C")
            // 24-50 (27 car): Descripcion articulo
            // 51-80 (30 car): Poligono de entrega y observaciones
            String sCodReg2 = "02";
            String sIdPedFijo2 = CUtilidadesCadena.rellenarDerecha(oPed.obtenerIdPedido(), 12);
            String sTipoFijo = CUtilidadesCadena.rellenarDerecha(oPed.obtenerTipoTornillo(), 2);
            String sCantidadFija = CUtilidadesCadena.rellenarIzquierdaCeros(oPed.obtenerCantidad(), 6);
            String sEmpaqueFijo = CUtilidadesCadena.rellenarDerecha(oPed.obtenerFormatoEmpaque(), 1);
            String sDescFija = CUtilidadesCadena.rellenarDerecha("TORNILLO METRICO INDUSTRIAL", 27);
            String sPoligonoFijo = CUtilidadesCadena.rellenarDerecha("POLIGONO INDUSTRIAL PINTO", 30);

            String sRegistro02 = sCodReg2 + sIdPedFijo2 + sTipoFijo + sCantidadFija + sEmpaqueFijo + sDescFija + sPoligonoFijo;

            // Retornar exactamente las dos lineas de 80 caracteres con separador de linea
            return sRegistro01 + "\n" + sRegistro02 + "\n";
        }

        // ----------------------------------------------------------------------
        // FORMATO 4: Segmentos EDI / ODETTE planos delimitados por tubería '|'
        // Proveedor: Suministros Navales del Norte S.A.
        // ----------------------------------------------------------------------
        private static String generarFormato4_EdiSegmentos(CProveedor oProv, CPedido oPed) {
            SimpleDateFormat oFmtFecha = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat oFmtHora = new SimpleDateFormat("HHmmss");
            String sFecha = oFmtFecha.format(oPed.obtenerFechaEmision());
            String sHora = oFmtHora.format(oPed.obtenerFechaEmision());

            return String.format("""
                    EMP|%s|%s|SANTANDER-PUERTO|ES
                    ORD|%s|%s|%s|NORMAL
                    LIN|01|%s|%d|%s|TORNILLO_ACERO_INOX_MARINO
                    TRA|CONTROL_CHECKSUM=VALIDO|TOTAL_ITEMS=1
                    """,
                    oProv.obtenerCif(),
                    oProv.obtenerNombre(),
                    oPed.obtenerIdPedido(),
                    sFecha,
                    sHora,
                    oPed.obtenerTipoTornillo(),
                    oPed.obtenerCantidad(),
                    oPed.obtenerFormatoEmpaque()
            );
        }

        // ----------------------------------------------------------------------
        // FORMATO 5: Propiedades B2B [SECCION] clave=valor
        // Proveedor: Electro-Mecanica Levante C.B.
        // ----------------------------------------------------------------------
        private static String generarFormato5_PropiedadesB2B(CProveedor oProv, CPedido oPed) {
            SimpleDateFormat oFmtFecha = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat oFmtHora = new SimpleDateFormat("HH:mm:ss");
            String sFecha = oFmtFecha.format(oPed.obtenerFechaEmision());
            String sHora = oFmtHora.format(oPed.obtenerFechaEmision());

            return String.format("""
                    [CABECERA_DOCUMENTO]
                    cif_empresa=%s
                    razon_social=%s
                    fecha_operacion=%s
                    hora_registro=%s

                    [DATOS_PEDIDO]
                    id_pedido_cliente=%s
                    tipo_tornillo=%s
                    cantidad_unidades=%d
                    embalaje_requerido=%s
                    centro_entrega=ALMACEN_VALENCIA_SUR
                    """,
                    oProv.obtenerCif(),
                    oProv.obtenerNombre(),
                    sFecha,
                    sHora,
                    oPed.obtenerIdPedido(),
                    oPed.obtenerTipoTornillo(),
                    oPed.obtenerCantidad(),
                    oPed.obtenerFormatoEmpaque()
            );
        }

        // ----------------------------------------------------------------------
        // FORMATO 6: Sintaxis Declarativa SCADA / Lenguaje de Comandos Industrial
        // Proveedor: Robotica y Ensamble Automatizado S.L.
        // ----------------------------------------------------------------------
        private static String generarFormato6_ScadaComandos(CProveedor oProv, CPedido oPed) {
            SimpleDateFormat oFmtTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String sTimestamp = oFmtTimestamp.format(oPed.obtenerFechaEmision());

            return String.format("""
                    SOLICITUD_PRODUCCION_SCADA_V2
                    EMISOR_CIF: %s
                    EMISOR_EMPRESA: REA_ROBOTICA
                    ORDEN_IDENTIFICADOR: %s
                    TIMESTAMP: %s
                    COMANDO_EJECUCION: FABRICAR
                    PARAMETRO_TIPO_TORNILLO: %s
                    PARAMETRO_CANTIDAD_TORNILLOS: %d
                    PARAMETRO_ENVASE_FINAL: %s
                    MODO_FABRICACION: CONTINUO
                    FIN_SOLICITUD
                    """,
                    oProv.obtenerCif(),
                    oPed.obtenerIdPedido(),
                    sTimestamp,
                    oPed.obtenerTipoTornillo(),
                    oPed.obtenerCantidad(),
                    oPed.obtenerFormatoEmpaque()
            );
        }
    }

    // ==========================================================================
    // CLASE AUXILIAR: UTILIDADES DE CADENA PARA ANCHO FIJO (NORMA 19)
    // ==========================================================================
    public static class CUtilidadesCadena {

        /**
         * Rellena un texto con espacios a la derecha hasta alcanzar la longitud fija.
         * Si supera la longitud, se trunca limpiamente.
         */
        public static String rellenarDerecha(String sTexto, int iLongitud) {
            if (sTexto == null) {
                sTexto = "";
            }
            if (sTexto.length() >= iLongitud) {
                return sTexto.substring(0, iLongitud);
            }
            StringBuilder oSb = new StringBuilder(sTexto);
            while (oSb.length() < iLongitud) {
                oSb.append(' ');
            }
            return oSb.toString();
        }

        /**
         * Formatea un entero con ceros a la izquierda hasta alcanzar la longitud deseada.
         */
        public static String rellenarIzquierdaCeros(int iNumero, int iLongitud) {
            String sNum = String.valueOf(iNumero);
            if (sNum.length() >= iLongitud) {
                return sNum.substring(0, iLongitud);
            }
            StringBuilder oSb = new StringBuilder();
            while (oSb.length() + sNum.length() < iLongitud) {
                oSb.append('0');
            }
            oSb.append(sNum);
            return oSb.toString();
        }
    }
}
