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

            int numFits = fits.getNumberOfHDUs();
            BasicHDU basicHdu;

            // Obtener el HDU principal.
            basicHdu = fits.getHDU(posicionHDU - 1);
            row.initRow("CUBE"); // Inicializa la fila.
            row.set(TEMPSCAL, "TMB"); // Valor extra por defecto.
            row.set(UNITVELO, "km/s"); // Unidades de velocidad por defecto.
            row.set(CTYPE3, "WAVE-LSR"); // Tipo espectral por defecto.

            // Procesar la cabecera principal (posicion 0).
            basicHdu = fits.getHDU(0);
            String origin = basicHdu.getHeader().getStringValue(MyConstants.FITS_HEADER_ORIGIN);

            processHead(basicHdu.getHeader()); // Procesar cabecera, inserta cards.

            // Procesa la posicion que le corresponda
            basicHdu = fits.getHDU(posicionHDU - 1);

            // Validar si el archivo es de CASA.
            if(origin==null)
                origin = basicHdu.getHeader().getStringValue(MyConstants.FITS_HEADER_ORIGIN);
            if (isFromCasa &&(origin == null || !origin.toUpperCase().contains("CASA")) )
            {

                showErrMnsg("This is not a CASA cube/spectra. Plese try to import it using the generic cube import");
                return null;
            }

            // Inserta Cards
            processHead(basicHdu.getHeader());

            // Obtiene las posiciones de desplazamiento en la imagen (el offset)
            if (basicHdu instanceof ImageHDU) {
                offset = ((ImageHDU) basicHdu).getFileOffset(); // recoge el del principio del HDU
                offset += basicHdu.getHeader().getOriginalSize(); // tamano de la cabecera
            } else {
                showErrMnsg("No Alma cube format found");
            }

            // Realocate crPix
            row.castCrpix();

            // Se realiza la validacion de unidades
            unitValidation(posicionHDU, row);

            // Si no se encuentran BMAJ, BMIN y BPA se obtienen de una cabecera alternativa
            Float bMajAux =row.getFloat(BMAJ);
            Float bMinAux =row.getFloat(BMIN);
            Float bpaAux = row.getFloat(BPA);
            if (Float.isNaN(bMajAux) || Float.isNaN(bMinAux) ||Float.isNaN(bpaAux) ||(bMajAux == 0 &&  bMinAux== 0 && bpaAux == 0)) {
                if (numFits < 2) {
                    showErrMnsg("No beam size imported");
                    return null;
                } else {
                    setBeamValues(fits, row,row.getDouble(CRPIX3)); //BUSCAR LO DE LA TABLA DESPUES DE ESTO SI SIGUE SIENDO NaN
                }
            }

            // Conversion de unidades
            unitConvert();
            unitCalculation();

            // Realocate crPix
            row.castCrpix();

            // Validacion de los parametros
            validateParameters(posicionHDU, row);

            // Declaracion placeholder para el nuevo fichero generado
            File inFile = new File(strFile);

            if (importedFilePath == null || importedFilePath.trim().length() == 0) {
                importedFilePath = inFile.getParent();
            }

            nameOut = importedFilePath + File.separator + MyConstants.NAME_GENERAL_CUBE + "_"
                    + inFile.getName();


            row.set(TELESCOP, MyConstants.TELESCOPE_ALMA_COMMON);// Siempre que se importe un cubo poner como COMON_BEAM el telescope
            Header hdr = row.getHeader();
            row. escribeCnsolaHeader(hdr);

            // Validacion de la velocidad en ALMA
            boolean radialVelo = true; // como se si tiene velocidad radial?

            // Inserta NAXIS
            int naxis = 0;
            for (; naxis < 10; naxis++) {
                if (hdr.getIntValue(MyConstants.FITS_HEADER_NAXIS + naxis) > 0) {

                } else if (naxis > 0) {
                    hdr.addValue(MyConstants.FITS_HEADER_NAXIS, naxis, "");

                    break;
                }
            }

            String auxKey = "";
            HeaderCard card;
            WCSGlobal wcsGlobal= new WCSGlobal(hdr);

            // ??
            if(listNewArgs!=null && !listNewArgs.equals("None")&& !listNewArgs.equals(""))
	    {
		newArgs = listNewArgs;
		AstronomicalUtilities.changeValueHeader(wcsGlobal,listNewArgs);
		hdr = wcsGlobal.getHeader();
	    }

            // ??
            if (origin != null && origin.contains("CASA") )
            {
	        double naxis3 = hdr.getDoubleValue(MyConstants.FITS_HEADER_NAXIS+3, Double.NaN);
            	if(radialVelo)
                {
	              // Validacion de velocidad radial
	              double altrValAux = hdr.getDoubleValue(MyConstants.FITS_HEADER_ALTRVAL, Double.NaN);
	              double altrPixAux = hdr.getDoubleValue(MyConstants.FITS_HEADER_ALTRPIX, Double.NaN);
	              //  Si tiene un solo axis no modificar
	              if( !Double.isNaN(naxis3) && naxis3 >= 2.0 && (Double.isNaN(altrValAux) || altrPixAux<2 ))
	              {
	            	  if(!isMacro){
	                    ImportFITSCubeAlmaVelocityPanel dialogo;
	                    dialogo = new ImportFITSCubeAlmaVelocityPanel(hdr);
	                    dialogo.init();
	                    String [] labels = dialogo.getLabelAxisUnit();
	                    String arg = "";

	                    if(labels ==null)
	                    {
		                    if (wcsGlobal.getWcsSpectral().getVelDef()
	                                .startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT))
		                    	arg = MyConstants.SPECTRAL_AXIS_TYPE_VOPT+"$";
		                    else   if (wcsGlobal.getWcsSpectral().getVelDef()
	                                .startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD))
		                    	arg = MyConstants.SPECTRAL_AXIS_TYPE_VRAD+"$";
		                    else
		                    	arg = "VELO$";
		                    arg +=hdr.getDoubleValue(MyConstants.FITS_HEADER_ALTRVAL)+"$m/s";
	                    } else {
	                    	arg = labels[0]+"$"+labels[1]+"$";
	                    	if(labels[2]!=null)
		                    	arg += labels[2];
	                    }
	                    newArgs += arg;

	            	 } else {
	            		 EditorInformationMADCUBA.append(inFile.getName()+": The radial velocity is out of range.");
	            	 }
	              }
                }


                //BEAM LOOK IN A TABLE
                bMajAux =row.getFloat(BMAJ);
                bMinAux =row.getFloat(BMIN);
                bpaAux = row.getFloat(BPA);

            	if (Float.isNaN(bMajAux) || Float.isNaN(bMinAux) ||Float.isNaN(bpaAux) ||
            			(bMajAux == 0 &&  bMinAux== 0 && bpaAux == 0))
           		{
  	              double altrPixAux = hdr.getDoubleValue(MyConstants.FITS_HEADER_ALTRPIX, Double.NaN);
  	              if(altrPixAux<1)
  	              {
  	            	  double valuePix =  naxis3/2D;
  	            	  if(valuePix>1)
  	            		  setBeamValues(fits, row, valuePix);

	            	  bMajAux =row.getFloat(BMAJ);
	            	  bMinAux =row.getFloat(BMIN);
	            	  bpaAux = row.getFloat(BPA);
  	              } else {
  	            	  int count =0;
  		        	  while((Float.isNaN(bMajAux) || Float.isNaN(bMinAux) ||Float.isNaN(bpaAux) ||
  	            			(bMajAux == 0 &&  bMinAux== 0 && bpaAux == 0))&& count<20)
  	            	  {
  	            		  setBeamValues(fits, row, altrPixAux);
  	            		  bMajAux =row.getFloat(BMAJ);
  	            		  bMinAux =row.getFloat(BMIN);
  	            		  bpaAux = row.getFloat(BPA);

  	            			altrPixAux++;
  	            			count++;
  	            	  }
  	              }
                  hdr.addValue(MyConstants.FITS_HEADER_BMAJ, bMajAux, "BMAJ FROM TABLE");
                  hdr.addValue(MyConstants.FITS_HEADER_BMIN, bMinAux, "BMIN FROM TABLE");
                  hdr.addValue(MyConstants.FITS_HEADER_BPA, bpaAux, "BPA FROM TABLE");
           		}
            }

            // Populate additional information
            hdr.addValue(MyConstants.FITS_HEADER_SIMPLE, true, "Standard FITS format");
            long oldBitPix = hdr.getIntValue(MyConstants.FITS_HEADER_BITPIX);
            hdr.addValue(MyConstants.FITS_HEADER_BITPIX, -32, "float type");

            // Generar archivo FITS modificado.
            generateNewCube(nameOut, namePlugin,hdr, dataInput, offset, oldBitPix);
            isOk = true;
        } catch (FitsException  | AxisUnknownException e) {
            showErrMnsg("Cannot open FITS File " + strFile+":\n"+e.getMessage());
            return null;
        } catch (IOException e) {
            showErrMnsg("Cannot open FITS File " + strFile+":\n"+e.getMessage());
            return null;
        }

        // Retornar información del archivo procesado.
        if(isOk)
        {
        	String sArg = "select='"+strFile+"'";
        	if(!newArgs.equals("None") && !newArgs.equals(""))
        		sArg += " changehead='"+newArgs+"'";
        	HistoryAndMacrosUtils.writeFileHistory(historyStarTable, 
        			HistoryAndMacrosUtils.FITS_TYPE_CUBE, namePlugin, sArg);
        	HistoryAndMacrosUtils.saveFileHistory(historyStarTable, HistoryAndMacrosUtils.FITS_TYPE_CUBE, nameOut);
        }

        // String[] salida = {nameOut,nameErrorOut, nameSigmaOut};

        String[][] salida = {{ nameOut }, {newArgs}};

        return salida;

    }

    // Métodos adicionales: validación de unidades, cálculo de sigma, conversión de datos, etc.

   /**
   * Cambia los valores de velocidad (radial, óptica, o desplazamiento al rojo) en la cabecera del archivo FITS.
   *
   * Este método procesa una lista de argumentos que contienen ajustes para las velocidades espectrales,
   * identificados por etiquetas específicas como `VELO`, `VRAD`, `VOPT`, o `REDSHIFT`. Los valores son
   * convertidos y actualizados en la cabecera del archivo FITS, garantizando la coherencia con el sistema
   * de coordenadas espectrales definido en el objeto `WCSGlobal`.
   * 
   * @param listNewArgs Lista de argumentos con cambios en los valores espectrales (en formato `etiqueta$valor$unidad`).
   * @param newArgs Cadena con los valores convertidos que se deben aplicar a la cabecera.
   * @param hdr Cabecera del archivo FITS donde se aplicarán los cambios.
   * @param wcsGlobal Objeto que gestiona el sistema de coordenadas espectrales y sus transformaciones.
   * @return Una cadena actualizada con los nuevos argumentos aplicados a la cabecera.
   */
    public String changeValuesVeloOld(String listNewArgs, String newArgs,
			Header hdr, WCSGlobal wcsGlobal) {
		if(listNewArgs!=null && !listNewArgs.equals("None")&& !listNewArgs.equals(""))
		{
			  // Inicializa la cadena de argumentos convertidos
                          newArgs = listNewArgs;

                          // Tokeniza los argumentos de entrada usando '#' como delimitador
			  StringTokenizer st= new StringTokenizer(listNewArgs, "#");
			  String valueToken = "";
			  int separatorIndex = -1;

                          // Obtiene el transformador espectral del sistema de coordenadas
			  SpectralTransformator spectTransformator = wcsGlobal.getWcsSpectral().getSt();

                          // Itera sobre los tokens
			  while(st.hasMoreTokens())
			  {
				valueToken = st.nextToken();

				  separatorIndex = valueToken.indexOf("$");
				  if(separatorIndex>-1)
				  {
                                        // Separa la etiqueta (ej: VELO, VRAD) del valor
		  			String valueLabel = valueToken.substring(0, separatorIndex);
		                        valueLabel = valueLabel.toUpperCase();
		  			if(valueLabel.equals("VELO") ||valueLabel.equals("VRAD") || valueLabel.equals("VOPT")
		  				 ||	valueLabel.equals("REDSHIFT"))
		  			{
		  				String valueData = valueToken.substring(separatorIndex+1);

                                                // Procesa unidades si están especificadas
		  				String valueUnit = null;
		  				separatorIndex = valueData.indexOf("$");
		  				if(separatorIndex>-1) //THEN HAS UNIT
		  				{
		  					valueUnit = valueData.substring(separatorIndex+1);
		  					valueData = valueData.substring(0, separatorIndex);
		  				}
		  				//NEED THREE VALUES (ALTVAL, ALTPIX AND ZSOURCE)
		  				//FIRTS LOOK VALUE UNIT 
		  				//TODO 
		  				//THEN INSERT VLAUE UNIT CORRECT IN UNIT CORRECT
		  				//CALCULATE OTHERS PARAMS

                                  // Convierte el valor dependiendo de la etiqueta
		                  double newValue = new Double(valueData);
		                  if(!valueLabel.equals("REDSHIFT") && valueUnit!=null && valueUnit.toLowerCase().startsWith("k")  	)//CHANGE UNIT CORRECTO LAS BASELINE OR CHANGE VALOCITY
		                	  newValue = newValue*1000;

		                 double[] values = null;

                                 // Realiza conversiones dependiendo del sistema espectral
		                 if (wcsGlobal.getWcsSpectral().getVelDef()
		                         .startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT))
		                 {
		                	 if(valueLabel.equals("REDSHIFT"))
		                		 newValue = UnitTransformator.tranfFromAnyChannelToOtherChannel(newValue,MyConstants.X_AXIS_REDSHIFT,
		                                 MyConstants.X_AXIS_VOPT,spectTransformator);
		                	 else if(!valueLabel.equals("VOPT"))
		                		 newValue = UnitTransformator.tranfFromAnyChannelToOtherChannel(newValue,MyConstants.X_AXIS_VRAD,
		                             MyConstants.X_AXIS_VOPT,spectTransformator);

                    		         values = wcsGlobal.getWcsSpectral().getSt().getValuesFromVeloOpt(newValue);

		                 } else if (wcsGlobal.getWcsSpectral().getVelDef()
		                         .startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD)) {
		                	 if(valueLabel.equals("REDSHIFT"))
		                		 newValue = UnitTransformator.tranfFromAnyChannelToOtherChannel(newValue,MyConstants.X_AXIS_REDSHIFT,
		                                 MyConstants.X_AXIS_VRAD,spectTransformator);

		                	 else if(valueLabel.equals("VOPT"))
		                		 newValue = UnitTransformator.tranfFromAnyChannelToOtherChannel(newValue,MyConstants.X_AXIS_VOPT,
		                             MyConstants.X_AXIS_VRAD,spectTransformator);

                                         values = wcsGlobal.getWcsSpectral().getSt().getValuesFromVeloRad(newValue);
		                 }

                                 // Actualiza la cabecera con los nuevos valores calculados
		                 try {
		                     hdr.addValue(MyConstants.FITS_HEADER_ALTRVAL, newValue, "MODIFY VELOCITY");
		                     hdr.addValue(MyConstants.FITS_HEADER_ALTRPIX, values[0], "MODIFY VELOCITY");
		                     hdr.addValue(MyConstants.FITS_HEADER_ZSOURCE, values[5], "MODIFY VELOCITY");
		                 } catch (HeaderCardException e1) {
		                     // TODO Auto-generated catch block
		                     System.out.println(e1.getMessage());
		                     e1.printStackTrace();
		                 }
		  	      }

		            }
			  }
		}
		return newArgs;
	}

       /**
       * Asigna un valor a una etiqueta en la fila de datos (`RowImport`) de acuerdo con su tipo.
       *
       * Este método determina el tipo de dato de un valor proporcionado y lo asigna a una etiqueta
       * específica en la fila de importación actual. Es útil para procesar cabeceras y datos de
       * archivos FITS donde los valores pueden ser de diferentes tipos (int, float, string, etc.).
       *
       * @param typeClass Tipo del valor (int, float, string, etc.) representado por un entero.
       * @param valueLabel Etiqueta a la que se asignará el valor en la fila de importación.
       * @param valueData Valor en formato de cadena que será convertido al tipo correspondiente.
       */
       public void setValue(int typeClass, String valueLabel, String valueData) {
           switch (typeClass) {
               case 0:
               // Caso de tipo String
               row.set(valueLabel, valueData);
               break;

               case 1:
               // Caso de tipo Integer
               row.set(valueLabel, Integer.parseInt(valueData));
               break;

               case 2:
               // Caso de tipo Short
               row.set(valueLabel, Short.parseShort(valueData));
               break;

               case 3:
               // Caso de tipo Float
               row.set(valueLabel, Float.parseFloat(valueData));
               break;

               case 4:
               // Caso de tipo Double
               row.set(valueLabel, Double.parseDouble(valueData));
               break;

               case 5:
               // Caso de tipo Long
               row.set(valueLabel, Long.parseLong(valueData));
               break;

               default:
               // Caso por defecto: asignar como String
               row.set(valueLabel, valueData);
           }
       }

       /**
       * Configura los valores del haz (beam) en la cabecera del archivo FITS.
       *
       * Este método extrae los valores del tamaño del haz (BMAJ, BMIN) y su orientación (BPA)
       * desde una tabla binaria en el archivo FITS. Si es necesario, convierte las unidades
       * de los valores a grados antes de asignarlos a la fila de datos (`RowImport`).
       *
       * @param fits Objeto `Fits` que contiene el archivo FITS abierto.
       * @param row Fila de datos (`RowImport`) donde se asignarán los valores del haz.
       * @param crpix3 Índice del eje espectral usado para localizar los valores del haz.
       */
       private void setBeamValues(Fits fits, RowImport row, double crpix3) {
           try {
               // Obtiene el HDU de tipo tabla binaria (posición 1).
               BinaryTableHDU hdu = (BinaryTableHDU) fits.getHDU(1);
               Header header = hdu.getHeader();

               // Obtiene los valores del haz desde la tabla binaria.
               float bMaj = ((float[]) hdu.getElement((int) crpix3, 0))[0]; // Tamaño mayor
               float bMin = ((float[]) hdu.getElement((int) crpix3, 1))[0]; // Tamaño menor
               float bpa = ((float[]) hdu.getElement((int) crpix3, 2))[0];  // Ángulo de posición

               // Si los valores del haz son inválidos
               if (bMaj == 1.0E-6 || bpa == 0.0) {

                   // Obtiene las unidades de los valores desde la cabecera.
                   String unitBMaj = header.getStringValue("TUNIT1");
                   String unitBMin = header.getStringValue("TUNIT2");
                   String unitBPA = header.getStringValue("TUNIT3");

                   // Convierte los valores a grados si las unidades están definidas.
                   Unit unitOriBMaj = Unit.parse(unitBMaj);
                   Unit unitOriBMin = Unit.parse(unitBMin);
                   Unit unitOriBPA = Unit.parse(unitBPA);

                   if (unitOriBMaj != null) {
                       bMaj = (float) unitOriBMaj.getValue((double) bMaj, Angle.DEGREES);
                   }
                   if (unitOriBMin != null) {
                       bMin = (float) unitOriBMin.getValue((double) bMin, Angle.DEGREES);
                   }
                   if (unitOriBPA != null) {
                       bpa = (float) unitOriBPA.getValue((double) bpa, Angle.DEGREES);
                   }

                   // Asigna los valores procesados al objeto `row`.
                   row.set(BMAJ, bMaj);
                   row.set(BMIN, bMin);
                   row.set(BPA, bpa);
               }
           } catch (FitsException e) {
               // Manejo de errores de FITS.
               e.printStackTrace();
           } catch (IOException e) {
               // Manejo de errores de entrada/salida.
               e.printStackTrace();
           }
       }







}
