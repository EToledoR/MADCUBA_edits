package es.cab.plugins.impor;

import static es.cab.plugins.impor.ImportConst.ALTRPIX;
import static es.cab.plugins.impor.ImportConst.ALTRVAL;
import static es.cab.plugins.impor.ImportConst.BMAJ;
import static es.cab.plugins.impor.ImportConst.BMIN;
import static es.cab.plugins.impor.ImportConst.BPA;
import static es.cab.plugins.impor.ImportConst.CDELT3;
import static es.cab.plugins.impor.ImportConst.CRPIX3;
import static es.cab.plugins.impor.ImportConst.CRVAL1;
import static es.cab.plugins.impor.ImportConst.CRVAL2;
import static es.cab.plugins.impor.ImportConst.CRVAL3;
import static es.cab.plugins.impor.ImportConst.CTYPE3;
import static es.cab.plugins.impor.ImportConst.IMAGEFREQ;
import static es.cab.plugins.impor.ImportConst.ORIGIN;
import static es.cab.plugins.impor.ImportConst.RESTFREQ;
import static es.cab.plugins.impor.ImportConst.RESTWAVE;
import static es.cab.plugins.impor.ImportConst.SPECRES;
import static es.cab.plugins.impor.ImportConst.SPECSYS;
import static es.cab.plugins.impor.ImportConst.TEMPSCAL;
import static es.cab.plugins.impor.ImportConst.UNITVELO;
import static es.cab.plugins.impor.ImportConst.VELDEF;
import static es.cab.plugins.impor.ImportConst.VELREF;
import static es.cab.plugins.impor.ImportConst.ZSOURCE;
import static es.cab.plugins.impor.ImportConst.TELESCOP;
import static es.cab.plugins.impor.RowImport.DEFAULT_SPECSYS;
import herschel.share.unit.Angle;
import herschel.share.unit.Unit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.FitsUtil;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedFile;
import nom.tam.util.Cursor;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.table.RowListStarTable;
import es.cab.astronomical.AstronomicalVirtualStack;
//import es.cab.astronomical.AstronomicalVirtualStackOld;
import es.cab.astronomical.utils.HistoryAndMacrosUtils;
import es.cab.madcuba.log.Log;
import es.cab.madcuba.tools.SpectralTransformator;
import es.cab.madcuba.utils.AstronomicalUtilities;
import es.cab.madcuba.utils.MyConstants;
import es.cab.madcuba.utils.MyUtilities;
import es.cab.madcuba.utils.UnitTransformator;
import es.cab.madcuba.utils.except.AxisUnknownException;
import es.cab.madcuba.wcs.WCSGlobal;
import es.cab.swing.gui.EditorInformationMADCUBA;
/**
 * Clase `ImportFITSCubeAlma`
 *
 * Esta clase importa y procesa cubos FITS provenientes de ALMA. Realiza validaciones de unidades,
 * conversiones, ajustes de cabeceras y finalmente genera un archivo FITS modificado compatible
 * con MADCUBA.
 *
 * Extiende la funcionalidad de la clase base `ImportFITS` y se enfoca en datos espectrales y 
 * espaciales específicos de cubos observados con ALMA.
 */
public class ImportFITSCubeAlma extends ImportFITS {

    /** Listas para almacenar coordenadas de ascensión recta y declinación. */
    protected List<Double> ascension;
    protected List<Double> declination;

    /** Columnas específicas para longitudes de onda y errores en el flujo. */
    protected int[] colWave = null;
    protected int[] colErrorFlux = null;

    /** Historial asociado al cubo FITS. */
    protected ArrayList<Object[]> historyStarTable;

    /**
     * Constructor principal que inicializa la clase con un historial de datos.
     *
     * @param historyStarTable Lista con el historial de datos del cubo FITS.
     */
    public ImportFITSCubeAlma(ArrayList<Object[]> historyStarTable) {
        super();
        this.historyStarTable = historyStarTable;
        FitsFactory.setUseHierarch(true); // Configuración para cabeceras HIERARCH en FITS.
    }

    /**
     * Constructor con opción de modo consola.
     *
     * @param historyStarTable Lista con el historial de datos del cubo FITS.
     * @param console Indica si se deben mostrar mensajes en consola.
     */
    public ImportFITSCubeAlma(ArrayList<Object[]> historyStarTable, boolean console) {
        super(console);
        this.historyStarTable = historyStarTable;
        FitsFactory.setUseHierarch(true);
    }

    /** Inicializa una fila de importación específica para ALMA. */
    @Override
    protected void init() {
        row = new RowImportCubeAlma(); // Configura la fila específica para cubos ALMA.
    }

    /**
     * Importa un cubo FITS.
     * 
     * @param strFile Ruta al archivo FITS.
     * @param importedFilePath Directorio de salida.
     * @param listNewArgs Argumentos adicionales para ajustar cabeceras.
     * @param namePlugin Nombre del plugin utilizado.
     * @param isMacro Indica si se ejecuta como macro.
     * @param isFromCasa Verifica si el archivo es originado por CASA.
     * @return Matriz con información del archivo procesado y argumentos.
     */
    public String[][] importCube(String strFile, String importedFilePath, String listNewArgs, 
                                 String namePlugin, boolean isMacro, boolean isFromCasa) {
        // Mensaje inicial.
        showStatusMnsg("Converting Spectral FITS to MADCUBAIJ\n");

        // Preparación de variables para el procesamiento.
        String newArgs = "";
        rowListStarTable = this.getNewRowListStarTable();
        boolean isOk = false;
        Fits fits;
        RandomAccess dataInput = null;
        long offset = 0;
        int posicionHDU = 1;
        String nameOut = null;

        try {
            // Abrir y leer el archivo FITS.
            fits = new Fits(strFile);
            dataInput = (RandomAccess) fits.getStream();
            fits.read(); // Lectura inicial.

            // Obtener el HDU principal.
            BasicHDU basicHdu = fits.getHDU(posicionHDU - 1);
            row.initRow("CUBE"); // Inicializa la fila.
            row.set(TEMPSCAL, "TMB"); // Escala de temperatura.
            row.set(UNITVELO, "km/s"); // Unidades de velocidad.
            row.set(CTYPE3, "WAVE-LSR"); // Tipo espectral.

            // Procesar la cabecera principal.
            basicHdu = fits.getHDU(0);
            String origin = basicHdu.getHeader().getStringValue(MyConstants.FITS_HEADER_ORIGIN);

            processHead(basicHdu.getHeader()); // Procesar cabecera.

            // Validar si el archivo es de CASA.
            if (isFromCasa && (origin == null || !origin.toUpperCase().contains("CASA"))) {
                showErrMnsg("This is not a CASA cube/spectra. Please try to import it using the generic cube import.");
                return null;
            }

            // Ajustar cabeceras y validar unidades.
            unitValidation(posicionHDU, row);
            unitConvert();
            unitCalculation();

            // Generar archivo FITS modificado.
            nameOut = generateNewCube(strFile, importedFilePath, fits, row);
            isOk = true;
        } catch (FitsException | IOException e) {
            showErrMnsg("Error opening FITS file: " + e.getMessage());
            return null;
        }

        // Retornar información del archivo procesado.
        if (isOk) {
            String[][] output = { { nameOut }, { newArgs } };
            return output;
        }

        return null;
    }

    // Métodos adicionales: validación de unidades, cálculo de sigma, conversión de datos, etc.
}
