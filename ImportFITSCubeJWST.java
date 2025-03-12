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

public class ImportFITSCubeJWST extends ImportFITS {

    protected List<Double> ascension;
    protected List<Double> declination;

    protected int[] colWave = null;
    protected int[] colErrorFlux = null;
    protected ArrayList<Object[]> historyStarTable;

    public ImportFITSCubeJWST(ArrayList<Object[]> historyStarTable) {
        super();
        this.historyStarTable = historyStarTable;
        FitsFactory.setUseHierarch(true);
    }

    public ImportFITSCubeJWST(ArrayList<Object[]> historyStarTable,boolean console) {
        super(console);
        this.historyStarTable = historyStarTable;
        FitsFactory.setUseHierarch(true);
    }

    @Override
    protected void init() {
        row = new RowImportCubeJWST();
    }
    

    public String[][] importCube(String strFile, String importedFilePath, String listNewArgs, String namePlugin, boolean isMacro, boolean isFromCasa) {
        showStatusMnsg("Converting Spectral FITS to MADCUBAIJ\n");
        String newArgs = "";
        rowListStarTable = this.getNewRowListStarTable();
        // No se si hace faltarow.initRow();
        // Abrir fichero FITS
        boolean isOk= false;
        Fits fits;
        RandomAccess dataInput = null;
        long offset = 0;
        // long offsetError = 0;
        int posicionHDU = 2;
        //int posicionHDU = 1;
        String nameOut = null;
        // String nameSigmaOut = null;
        // String nameErrorOut = null;

        try {
            fits = new Fits(strFile);
            dataInput = (RandomAccess) fits.getStream();
            fits.read(); // es necesario leer el fits

            int numFits = fits.getNumberOfHDUs();

            BasicHDU basicHdu;

            basicHdu = fits.getHDU(posicionHDU - 1);

            // Se realiza esto una vez por row dntro del mismo HDU
            // de aqui se obtienen n spectros que hay que subdividir
            // binaryTableHdu.getNRows()
            row.initRow("CUBE");
            row.set(TEMPSCAL, "TMB");// Valor extra por defecto
            row.set(UNITVELO, "km/s");// Valor extra por defecto
            row.set(CTYPE3, "WAVE-LSR");// Valor extra por defecto


            // ///////////////////////
            // Eduardo Toledo Feb 2025
            // Procesa la posicion 1
            // En los cubos de JWST se salta la posición 0 
            // por no ser relevante.
            // ///////////////////////
            basicHdu = fits.getHDU(1);
            String origin = basicHdu.getHeader().getStringValue(MyConstants.FITS_HEADER_ORIGIN);

            // Inserta Cards
            processHead(basicHdu.getHeader());

           // /////////////////////////////////////
            // Procesa la posicion que corresponda
            // /////////////////////////////////////
            basicHdu = fits.getHDU(posicionHDU - 1);
            
            if(origin==null)
                origin = basicHdu.getHeader().getStringValue(MyConstants.FITS_HEADER_ORIGIN);
            //if (isFromCasa &&(origin == null || !origin.toUpperCase().contains("CASA")) )
            //{

                //showErrMnsg("This is not a CASA cube/spectra. Plese try to import it using the generic cube import");
                //return null;
            //}
            
            // Inserta Cards
            processHead(basicHdu.getHeader());

            // Cambia los ejes
            // changeAxis();

            // Obtiene las posiciones de desplazamiento en la imagen

            if (basicHdu instanceof ImageHDU) {
                offset = ((ImageHDU) basicHdu).getFileOffset(); // recoge el del   // principio del              // HDU
                offset += basicHdu.getHeader().getOriginalSize(); // tamannio de  // la cabecera
            } else {
                showErrMnsg("No right cube format found");
            }

            // // Obtiene el offset del error
            // basicHdu = fits.getHDU(posicionHDU);
            // if (basicHdu instanceof ImageHDU) {
            // offsetError = ((ImageHDU) basicHdu).getFileOffset(); //recoge el
            // del principio del HDU
            // offsetError += basicHdu.getHeader().getOriginalSize(); //
            // tamannio de la cabecera
            // } else {
            // showErrMnsg("No Alma cube format found");
            // }

            // Realocate crPix
            row.castCrpix();

            // Se realiza la validacion de unidades
            unitValidation(posicionHDU, row);
            
            //NEED CHANGE VALUES FROM ARGUMENTS
            
            
            // Si no se encuentran BMAJ, BMIN y BPA se obtienen de una cabecera
            // alternativa
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

            // Se realiza la conversion de unidades
            unitConvert();

            unitCalculation();

            // Realocate crPix
            row.castCrpix();

            // Se validan los parametros
            validateParameters(posicionHDU, row);

            // Genera el nuevo ficheros
            // Genera el nuevo ficheros

            File inFile = new File(strFile);

            if (importedFilePath == null || importedFilePath.trim().length() == 0) {
                importedFilePath = inFile.getParent();
            }

            nameOut = importedFilePath + File.separator + MyConstants.NAME_GENERAL_CUBE + "_"
                    + inFile.getName();
            // nameSigmaOut = importedFilePath + File.separator +
            // MyConstants.NAME_GENERAL_SIGMA
            // + "_" + inFile.getName();
            // nameErrorOut = importedFilePath + File.separator +
            // MyConstants.NAME_GENERAL_ERROR
            // + "_" + inFile.getName();

            row.set(TELESCOP, MyConstants.TELESCOPE_ALMA_COMMON);// SIempre que se importe un cubo poner como COMON_BEAM el telescope
            Header hdr = row.getHeader();
//            row. completeHeader(hdrOrig,hdr);
            
            row. escribeCnsolaHeader(hdr);
            // Validacion Velocidad en ALMA
            // Llama a una clase copiada de ModifyDataPlugin, pero sin que sea
            // un plugin, ya que no hay ningun fichero creado aun

        //    String telescope = hdr.getStringValue(MyConstants.FITS_HEADER_TELESCOPE);

            
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

          ///CHANGE UNIT VELO WHEN INDICATED IN PARAMAS
            String auxKey = "";
            HeaderCard card;
//
//            Log.getInstance().logger.debug("HEADER1111111");
//            try {
//                //  OBject
//                Cursor ite = hdr.iterator();
//
//                while (ite.hasNext()) {
//                	card= (HeaderCard) ite.next();
//                	auxKey = card.getKey();
//                    Log.getInstance().logger.debug("ALL:"+auxKey+"--"+card.getValue());
//                }
//            
//            }catch (Exception e) {
//				// TODO: handle exception
//			}
//            Log.getInstance().logger.debug("FIN   HEADER1111111");
			WCSGlobal wcsGlobal= new WCSGlobal(hdr);
//			 Log.getInstance().logger.debug("HEADER2222222");
//	            try {
//	                //  OBject
//	                Cursor ite = hdr.iterator();
//
//	                while (ite.hasNext()) {
//	                	card= (HeaderCard) ite.next();
//	                	auxKey = card.getKey();
//	                    Log.getInstance().logger.debug("ALL:"+auxKey+"--"+card.getValue());
//	                }
//	            
//	            }catch (Exception e) {
//					// TODO: handle exception
//				}
//	            Log.getInstance().logger.debug("FIN   HEADER222222");
			if(listNewArgs!=null && !listNewArgs.equals("None")&& !listNewArgs.equals(""))
			{
				  newArgs = listNewArgs;
				  AstronomicalUtilities.changeValueHeader(wcsGlobal,listNewArgs);
				  hdr = wcsGlobal.getHeader();
			}
			
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
	            	//	  EditorInformationMADCUBA.append("The radial velocity is out of range. Set a new radial velocity");
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

            hdr.addValue(MyConstants.FITS_HEADER_SIMPLE, true, "Standard FITS format");

            long oldBitPix = hdr.getIntValue(MyConstants.FITS_HEADER_BITPIX);
            hdr.addValue(MyConstants.FITS_HEADER_BITPIX, -32, "float type");

            generateNewCube(nameOut, namePlugin,hdr, dataInput, offset, oldBitPix);
            // generateNewSigma(nameErrorOut,nameSigmaOut, hdr, dataInput,
            // offsetError, oldBitPix);
            isOk = true;
        } catch (FitsException  | AxisUnknownException e) {
            showErrMnsg("Cannot open FITS File " + strFile+":\n"+e.getMessage());
            return null;
        } catch (IOException e) {
            showErrMnsg("Cannot open FITS File " + strFile+":\n"+e.getMessage());
            return null;
        }

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

	public String changeValuesVeloOld(String listNewArgs, String newArgs,
			Header hdr, WCSGlobal wcsGlobal) {
		if(listNewArgs!=null && !listNewArgs.equals("None")&& !listNewArgs.equals(""))
		{
			  newArgs = listNewArgs;
			  StringTokenizer st= new StringTokenizer(listNewArgs, "#");
			  String valueToken = "";
			  int separatorIndex = -1;

			  SpectralTransformator spectTransformator = wcsGlobal.getWcsSpectral().getSt();
			  while(st.hasMoreTokens())
			  {
				valueToken = st.nextToken();

				  separatorIndex = valueToken.indexOf("$");
				  if(separatorIndex>-1)
				  {
		  			String valueLabel = valueToken.substring(0, separatorIndex);
		            valueLabel = valueLabel.toUpperCase();
		  			if(valueLabel.equals("VELO") ||valueLabel.equals("VRAD") || valueLabel.equals("VOPT")
		  				 ||	valueLabel.equals("REDSHIFT"))
		  			{
		  			//	int typeClass =  row.getTypeObject(MyConstants.FITS_HEADER_ALTRVAL);
		  				String valueData = valueToken.substring(separatorIndex+1);

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
		  			//	setValue(typeClass,MyConstants.FITS_HEADER_ALTRVAL,  valueData);
		  				
		  				//CALCULATE OTHERS PARAMS

		                  double newValue = new Double(valueData);
		                  if(!valueLabel.equals("REDSHIFT") && valueUnit!=null && valueUnit.toLowerCase().startsWith("k")  	)//CHANGE UNIT CORRECTO LAS BASELINE OR CHANGE VALOCITY
		                	  newValue = newValue*1000;

		                 double[] values = null;

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

		                 // -->wcsGlobal.getWcsSpectral().changeVelocity(value,
		                 // values[0], values[5]);

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

	public void setValue(int typeClass, String valueLabel, String valueData) {
		switch (typeClass) {
			case 0:
				row.set(valueLabel, valueData);
			
			break;

		case 1:

			row.set(valueLabel, new Integer(valueData));
			break;
		case 2:
			row.set(valueLabel, new Short(valueData));
		
			break;
		case 3:
			row.set(valueLabel, new Float( valueData));
		
			break;
		case 4:
			row.set(valueLabel, new Double(valueData));
		
			break;
		case 5:
			row.set(valueLabel, new Long(valueData));
		
			break;
      			/*	case 6:
			row.set(valueLabel, valueData);
		
			break;*/

		default:

			row.set(valueLabel, valueData);
		}
	}

    private void setBeamValues(Fits fits, RowImport row, double crpix3) {

        try {

        	// Eduardo Toledo Feb 2025
        	// La binary table en los cubos de JWST está en el 5 o 6
            BinaryTableHDU hdu = (BinaryTableHDU) fits.getHDU(6);

            Header header = hdu.getHeader();

       //     double crpix3 = row.getDouble(CRPIX3);
       //     if(crpix3<1)
            	

            // Valores
            float bMaj = ((float[]) hdu.getElement((int) crpix3, 0))[0];
            float bMin = ((float[]) hdu.getElement((int) crpix3, 1))[0];
            float bpa = ((float[]) hdu.getElement((int) crpix3, 2))[0];
            if(!(bMaj==1.0E-6 || bpa==0.0))
            {
	            String unitBMaj = header.getStringValue("TUNIT1");
	            String unitBMin = header.getStringValue("TUNIT2");
	            String unitBPA = header.getStringValue("TUNIT3");
	
	            // Conversion a grados
	            Unit unitOriBMaj = Unit.parse(unitBMaj);
	            Unit unitOriBMin = Unit.parse(unitBMin);
	            Unit unitOriBPA = Unit.parse(unitBPA);
	
	            if (unitOriBMaj != null) {
	                // bMaj = (float)Angle.DEGREES.getValue((double)bMaj,
	                // unitOriBMaj);
	
	                bMaj = (float) unitOriBMaj.getValue((double) bMaj, Angle.DEGREES);
	
	            }
	
	            if (unitOriBMin != null) {
	                // bMin = (float)Angle.DEGREES.getValue((double)bMin,
	                // unitOriBMin);
	                bMin = (float) unitOriBMin.getValue((double) bMin, Angle.DEGREES);
	            }
	
	            if (unitOriBPA != null) {
	                // bpa = (float)Angle.DEGREES.getValue((double)bpa, unitOriBPA);
	                bpa = (float) unitOriBPA.getValue((double) bpa, Angle.DEGREES);
	            }

	            row.set(BMAJ, bMaj);
	            row.set(BMIN, bMin);
	            row.set(BPA, bpa);
            }
        } catch (FitsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void generateNewCube(String name, String namePlugin,Header hdr, RandomAccess dataInput, long offset,
            long oldBitPix) {

        AstronomicalVirtualStack astroStack;
        BufferedFile s;
        int naxis1;
        int naxis2;
        Object readArray = null;

        try {
//            hdr.addValue(MyConstants.FITS_HEADER_TELESCOPE, MyConstants.TELESCOPE_IMPORT_HERSCHEL,
//                    "IMPORT FROM HERSCHEL");
            hdr.addValue(MyConstants.FITS_HEADER_SIMPLE, true, "Standard FITS format");
            naxis1 = hdr.getIntValue(MyConstants.FITS_HEADER_NAXIS + "1");
            naxis2 = hdr.getIntValue(MyConstants.FITS_HEADER_NAXIS + "2");
            int readSize = naxis1 * naxis2;
            hdr.addValue(MyConstants.FITS_HEADER_BITPIX, -32, "float type"); // BITPIX
                                                                             // --->
                                                                             // ojo
                                                                             // con
                                                                             // las
                                                                             // correcciones

            row.completeHeader(hdrOrig, hdr);
            //PORQUE NO LLAMA AL NUEVO
            astroStack = new AstronomicalVirtualStack(naxis1, naxis2,new WCSGlobal(hdr));

            File myfile = new File(name);
            if (myfile.exists()) {
                myfile.delete();
            }
            myfile.createNewFile();

            s = new BufferedFile(myfile, "rw");
            synchronized (s) {
                // Escribe la nueva cabecera

                int numSlices = astroStack.getSize();
               double minSigma = calulateSigma(dataInput, offset, oldBitPix, readSize, numSlices);
                hdr.addValue(MyConstants.FITS_HEADER_SIGMA, minSigma, "import");

//                row.completeHeader(hdrOrig, hdr);
                astroStack.getWCSGlobal().getHeader().addValue(MyConstants.FITS_HEADER_SIGMA, minSigma, "import");
            	
                hdr.write(s);
                s.flush();
                float[] data;

                dataInput.seek(offset);

                // Escribe los datos en formato float[]
                
              //  ((slice * readSize) * Math.abs(oldBitPix / 8))
                long valueOffsetTemp = 0;
                long longOldBitPix8 = (long)Math.abs(oldBitPix / 8);
                long longSlice = 0;
                long longReadSize = (long)readSize;
                long longSliceReadSize = 0;
                 
                 
                 for (int slice = 0; slice < numSlices; slice++) {
                     data = null;

                     // posicionarse en el fichero EN BYTES.
                     // Posicion = datos de ofset, + plano en el que estamos +
                     // posicion del ROI + linea del ROI leida

                     

                //     Log.getInstance().logger.debug("SEEK="+(offset + ((slice * readSize) * Math.abs(oldBitPix / 8))));       
                     
                     //NEED PARSE THE VALUE TO LONG
                     longSlice = (long)slice;
                     longSliceReadSize = longSlice*longReadSize;
                     valueOffsetTemp = offset + longSliceReadSize * longOldBitPix8;
                     dataInput.seek(valueOffsetTemp);

                     // 8: Byte
                     // 16: Short
                     // 32: Integer
                     // -32: Float
                     // -64: Double
                     if (oldBitPix == -64) {
                         readArray = (double[]) ArrayFuncs.newInstance(double.class, readSize);                        
                         dataInput.read((double[]) readArray, 0, readSize);
                         data = MyUtilities.toFloat((double[]) readArray);
                     } else if (oldBitPix == -32) {
                         readArray = (float[]) ArrayFuncs.newInstance(float.class, readSize);
                    //     Log.getInstance().logger.debug("readSize="+readSize);
                     //    Log.getInstance().logger.debug("readArray="+readArray);
                         
                         dataInput.read((float[]) readArray, 0, readSize);
                         data = MyUtilities.toFloat((float[]) readArray);
                         // data = (float[]) readArray;
                     } else if (oldBitPix == 32) {
                         readArray = (int[]) ArrayFuncs.newInstance(int.class, readSize);                        
                         dataInput.read((int[]) readArray, 0, readSize);
                         data = MyUtilities.toFloat((int[]) readArray);
                     } else if (oldBitPix == 8) {
                         readArray = (byte[]) ArrayFuncs.newInstance(byte.class, readSize);                        
                         dataInput.read((byte[]) readArray, 0, readSize);
                         data = MyUtilities.toFloat((byte[]) readArray);
                     } else if ((oldBitPix == 16)) {
                         readArray = (short[]) ArrayFuncs.newInstance(short.class, readSize);                        
                         dataInput.read((short[]) readArray, 0, readSize);
                         data = MyUtilities.toFloat((short[]) readArray);
                     } else {
                         Log.logger.error("Unknown base class");
                     }

                     if (data != null) {

                 //           Log.getInstance().logger.debug("suma:"+suma);
                //            Log.getInstance().logger.debug("sumaCuadrado:"+sumaCuadrado);
                    
//                         HistoryAndMacrosUtils.writeFileHistory(historyStarTable, HistoryAndMacrosUtils.FITS_TYPE_CUBE,"Import_CUBE_ALMA_FITS_FILE", 
//                                                                "sigma(" + slice + "): " + vectorSigma[slice]);
//                         
//                         
//                         System.out.println("sigma(" + slice + "): " + vectorSigma[slice]);
//                    
                         
                         //  float[] vectorSigma = new float[numSlices];
                         // for(float val:data){
                         //   sumaCuadrado +=(val*val);
                         //   suma +=val;
                         //}
                         // vectorSigma[slice]=  Math.sqrt(sumaCuadrado/numSlices  - (Math.pow(suma)));
//                        if(data!=null)
//                         {
//                         	float data1 = data[0];
//                         	float data2 = data[1];
//                         	if(Math.abs(data1) >1 && Math.abs(data2)>1)
//                         	{
//                                 Log.getInstance().logger.debug("SEEK="+(offset + ((slice * readSize) * Math.abs(oldBitPix / 8))));   
//                         	}
//                         } 
                         s.write(data);
                         s.flush();
                     }
                 }
//                 row.addLabelValue(MyConstants.FITS_HEADER_SIGMA, minSigma);
                HistoryAndMacrosUtils.writeFileHistory(HistoryAndMacrosUtils.FITS_TYPE_CUBE,historyStarTable, "INFO: "+namePlugin+", ESTIMATED_SIGMA=" +minSigma, -1,-1,-1, true);
                hdr.addValue(MyConstants.FITS_HEADER_SIGMA, minSigma, "IMPORT");

                row.completeHeader(hdrOrig, hdr);
                
                
                //  Minimo de vectorSigma y se escribe en la historia
                //double[] vectorSigma ;
          
                // Rellena con padding lo que falta
                FitsUtil.pad(s, s.getFilePointer());
                // Si no funciona lo de arriba probar con
                // byte[] padding = new byte[FitsUtil.padding((int)
                // s.getFilePointer())];
               
                // s.write(padding);
                s.flush();

                // Cierra el fichero
                s.close();
            }
        } catch (HeaderCardException e) {
            showErrMnsg("Cannot import FITS Cube " + name + " error inserting cards in file");
            System.out.println(e.getMessage());
        } catch (FitsException e) {
            showErrMnsg("Cannot import FITS Cube " + name + " error in FITS file");
            System.out.println(e.getMessage());
        } catch (IOException e) {
            showErrMnsg("Cannot import FITS Cube " + name);
            e.printStackTrace();
            System.out.println(e.getMessage());
        }catch (AxisUnknownException e) {
            showErrMnsg("Spatial coordinate system not supported");
            System.out.println(e.getMessage());

        }
    }

	private double calulateSigma(RandomAccess dataInput, long offset, long oldBitPix, int readSize,
			int numSlices)
			throws IOException {
		Object readArray;
        double minSigma = Double.MAX_VALUE;
        double[] vectorSigma = new double[numSlices];
		float[] data;
		long valueOffsetTemp;
		long longSlice;
		long longSliceReadSize;
        long longOldBitPix8 = (long)Math.abs(oldBitPix / 8);
        long longReadSize = (long)readSize;
		for (int slice = 0; slice < numSlices; slice++) {
		    data = null;

		    // posicionarse en el fichero EN BYTES.
		    // Posicion = datos de ofset, + plano en el que estamos +
		    // posicion del ROI + linea del ROI leida

		    

            //     Log.getInstance().logger.debug("SEEK="+(offset + ((slice * readSize) * Math.abs(oldBitPix / 8))));       
		    
		    //NEED PARSE THE VALUE TO LONG
		    longSlice = (long)slice;
		    longSliceReadSize = longSlice*longReadSize;
		    valueOffsetTemp = offset + longSliceReadSize * longOldBitPix8;
		    dataInput.seek(valueOffsetTemp);

		    // 8: Byte
		    // 16: Short
		    // 32: Integer
		    // -32: Float
		    // -64: Double
		    if (oldBitPix == -64) {
		        readArray = (double[]) ArrayFuncs.newInstance(double.class, readSize);                        
		        dataInput.read((double[]) readArray, 0, readSize);
		        data = MyUtilities.toFloat((double[]) readArray);
		    } else if (oldBitPix == -32) {
		        readArray = (float[]) ArrayFuncs.newInstance(float.class, readSize);
		   //     Log.getInstance().logger.debug("readSize="+readSize);
		    //    Log.getInstance().logger.debug("readArray="+readArray);
		        
		        dataInput.read((float[]) readArray, 0, readSize);
		        data = MyUtilities.toFloat((float[]) readArray);
		        // data = (float[]) readArray;
		    } else if (oldBitPix == 32) {
		        readArray = (int[]) ArrayFuncs.newInstance(int.class, readSize);                        
		        dataInput.read((int[]) readArray, 0, readSize);
		        data = MyUtilities.toFloat((int[]) readArray);
		    } else if (oldBitPix == 8) {
		        readArray = (byte[]) ArrayFuncs.newInstance(byte.class, readSize);                        
		        dataInput.read((byte[]) readArray, 0, readSize);
		        data = MyUtilities.toFloat((byte[]) readArray);
		    } else if ((oldBitPix == 16)) {
		        readArray = (short[]) ArrayFuncs.newInstance(short.class, readSize);                        
		        dataInput.read((short[]) readArray, 0, readSize);
		        data = MyUtilities.toFloat((short[]) readArray);
		    } else {
		        Log.logger.error("Unknown base class");
		    }

		    if (data != null) {
		          float suma = 0;
		          float sumaCuadrado = 0;
		          int i = 0;
		          boolean isAllNaN=true;

		           for(float val:data){
		        /*	   if(i==0)
		        	   {
		                   Log.getInstance().logger.debug("val:"+val);
		        	   }*/
		               if(Float.isNaN(val)){
		                   continue;
		               }
		               isAllNaN=false;
		               i++;
		             sumaCuadrado +=(val*val);
		             suma +=val;
		          }
		//           Log.getInstance().logger.debug("suma:"+suma);
            //            Log.getInstance().logger.debug("sumaCuadrado:"+sumaCuadrado);
		        vectorSigma[slice] = calculateStdDev(i, suma, sumaCuadrado);
		//        Log.getInstance().logger.debug("minSigma:"+minSigma);
		 //       Log.getInstance().logger.debug("vectorSigma[slice]:"+vectorSigma[slice]);
		        if(!isAllNaN)
		        	minSigma  = minSigma > vectorSigma[slice]?vectorSigma[slice]:minSigma;		       
		    }
		}
		return minSigma;
	}

//    private void generateNewSigma(String errorName, String sigmaName, Header hdr,
//            RandomAccess dataInput, long offset, long oldBitPix) {
//
//        Header hdrError = null;
//        Header hdrSigma = null;
//
//        AstronomicalVirtualStack astroStack;
//        BufferedFile bufferedSigmaFile;
//        BufferedFile bufferedErrorFile;
//        int naxis1;
//        int naxis2;
//        Object readArray = null;
//        int[] numElemErrArray;
//        float[] errArray;
//
//        try {
//            hdrError = hdr;
//            hdrSigma = hdrError.clone();
//
//            hdrSigma.addValue(MyConstants.FITS_HEADER_NAXIS + 3, 1, "MADCUBA");
//
//            naxis1 = hdrError.getIntValue(MyConstants.FITS_HEADER_NAXIS + "1");
//            naxis2 = hdrError.getIntValue(MyConstants.FITS_HEADER_NAXIS + "2");
//            int readSize = naxis1 * naxis2;
//
//            numElemErrArray = new int[readSize];
//            errArray = new float[readSize];
//
//            // Inicializacion de los arrays
//            for (int i = 0; i < readSize; i++) {
//                numElemErrArray[i] = 0;
//                errArray[i] = 0;
//            }
//
//            astroStack = new AstronomicalVirtualStack(naxis1, naxis2,new WCSGlobal(hdrError)); //NECESARIO CAMBIA CABECERA
//
//            File fileSigma = new File(sigmaName);
//            File fileError = new File(errorName);
//
//            if (fileSigma.exists()) {
//                fileSigma.delete();
//            }
//            fileSigma.createNewFile();
//
//            if (fileError.exists()) {
//                fileError.delete();
//            }
//            fileError.createNewFile();
//
//            bufferedSigmaFile = new BufferedFile(fileSigma, "rw");
//            bufferedErrorFile = new BufferedFile(fileError, "rw");
//
//            synchronized (bufferedSigmaFile) {
//                // Escribe la nueva cabecera
//                hdrError.write(bufferedErrorFile);
//
//                // hdr.addValue(MyConstants.FITS_HEADER_NAXIS + 3 , 1,
//                // "MADCUBA");
//                hdrSigma.write(bufferedSigmaFile);
//
//                bufferedSigmaFile.flush();
//                bufferedErrorFile.flush();
//                float[] data;
//
//                dataInput.seek(offset);
//
//                // Escribe los datos en formato float[]
//                int numSlices = astroStack.getSize();
//                long valueOffsetTemp = 0;
//                long longOldBitPix8 = (long)Math.abs(oldBitPix / 8);
//                long longSlice = 0;
//                long longReadSize = (long)readSize;
//                long longSliceReadSize = 0;
//                for (int slice = 0; slice < numSlices; slice++) {
//                    data = null;
//
//
//                    longSlice = (long)slice;
//                    longSliceReadSize = longSlice*longReadSize;
//                    valueOffsetTemp = offset + longSliceReadSize * longOldBitPix8;
//                    dataInput.seek(valueOffsetTemp);
////                    dataInput.seek(offset + ((slice * readSize) * Math.abs(oldBitPix / 8)));
//
//                    // 8: Byte
//                    // 16: Short
//                    // 32: Integer
//                    // -32: Float
//                    // -64: Double
//                    if (oldBitPix == -64) {
//                        readArray = (double[]) ArrayFuncs.newInstance(double.class, readSize);
//                        dataInput.read((double[]) readArray, 0, readSize);
//                        data = MyUtilities.toFloat((double[]) readArray);
//                    } else if (oldBitPix == -32) {
//                        readArray = (float[]) ArrayFuncs.newInstance(float.class, readSize);
//                        dataInput.read((float[]) readArray, 0, readSize);
//                        data = MyUtilities.toFloat((float[]) readArray);
//                        // data = (float[]) readArray;
//                    } else if (oldBitPix == 32) {
//                        readArray = (int[]) ArrayFuncs.newInstance(int.class, readSize);
//                        dataInput.read((int[]) readArray, 0, readSize);
//                        data = MyUtilities.toFloat((int[]) readArray);
//                    } else if (oldBitPix == 8) {
//                        readArray = (byte[]) ArrayFuncs.newInstance(byte.class, readSize);
//                        dataInput.read((byte[]) readArray, 0, readSize);
//                        data = MyUtilities.toFloat((byte[]) readArray);
//                    } else if ((oldBitPix == 16)) {
//                        readArray = (short[]) ArrayFuncs.newInstance(short.class, readSize);
//                        dataInput.read((short[]) readArray, 0, readSize);
//                        data = MyUtilities.toFloat((short[]) readArray);
//                    } else {
//                        Log.logger.error("Unknown base class");
//                    }
//
//                    // Guarda los valores en el fichero de errores
//                    if (data != null) {
//                        bufferedErrorFile.write(data);
//                        bufferedErrorFile.flush();
//                    }
//
//                    // Suma valores de error
//                    for (int i = 0; i < readSize; i++) {
//                        if (!Float.isNaN(data[i])) {
//                            numElemErrArray[i]++;
//                            errArray[i] += data[i];
//                        }
//
//                    }
//
//                }
//
//                // Realiza la media
//                for (int i = 0; i < readSize; i++) {
//                    errArray[i] /= numElemErrArray[i];
//
//                }
//
//                // Escritura en disco
//                if (numElemErrArray != null) {
//                    bufferedSigmaFile.write(errArray);
//                    bufferedSigmaFile.flush();
//
//                }
//
//                // Rellena con padding lo que falta
//                FitsUtil.pad(bufferedSigmaFile, bufferedSigmaFile.getFilePointer());
//                FitsUtil.pad(bufferedErrorFile, bufferedErrorFile.getFilePointer());
//
//                // Si no funciona lo de arriba probar con
//                // byte[] padding = new byte[FitsUtil.padding((int)
//                // s.getFilePointer())];
//                // s.write(padding);
//                bufferedSigmaFile.flush();
//                bufferedErrorFile.flush();
//
//                // Cierra el fichero
//                bufferedSigmaFile.close();
//                bufferedErrorFile.close();
//            }
//        } catch (HeaderCardException e) {
//            showErrMnsg("Cannot import FITS Cube  error inserting cards in error/sigma file");
//            System.out.println(e.getMessage());
//        } catch (FitsException e) {
//            showErrMnsg("Cannot import FITS Cube error in error/sigma FITS file");
//            System.out.println(e.getMessage());
//        } catch (IOException e) {
//            showErrMnsg("Cannot import FITS Cube");
//            System.out.println(e.getMessage());
//        } catch (CloneNotSupportedException e) {
//            showErrMnsg("Cannot import FITS Cube");
//            System.out.println(e.getMessage());
//        } catch (AxisUnknownException e) {
//            showErrMnsg("Spatial coordinate system not supported");
//            System.out.println(e.getMessage());
//        }
//    }

    protected List<Double> objArr2List(Object arrayObject) {
        List<Double> salida = new ArrayList<Double>();

        if (arrayObject instanceof byte[]) {
            byte[] arrByte = (byte[]) arrayObject;

            for (byte elem : arrByte) {
                salida.add(new Double(elem));
            }
        } else if (arrayObject instanceof short[]) {
            short[] arrShort = (short[]) arrayObject;

            for (short elem : arrShort) {
                salida.add(new Double(elem));
            }

        } else if (arrayObject instanceof int[]) {
            int[] arrInt = (int[]) arrayObject;

            for (int elem : arrInt) {
                salida.add(new Double(elem));
            }

        } else if (arrayObject instanceof long[]) {
            long[] arrLong = (long[]) arrayObject;

            for (Long elem : arrLong) {
                salida.add(new Double(elem));
            }

        } else if (arrayObject instanceof float[]) {

            float[] arrFloat = (float[]) arrayObject;

            for (float elem : arrFloat) {
                salida.add(new Double(elem));
            }

        } else if (arrayObject instanceof double[]) {
            double[] arrDouble = (double[]) arrayObject;

            for (double elem : arrDouble) {
                salida.add(new Double(elem));
            }
        } else {
            showErrMnsg("No BitPix defined to manage Image Data");
        }

        return salida;
    }

    @Override
    protected void unitCalculation() {
        // String coordType = row.getString(COORDTYPE).toUpperCase();
        //
        // String coordProj = row.getString(COORDPROJ).toUpperCase();

        // /////////
        // ORIGIN
        // /////////
        row.set(ORIGIN, "MADCUBA");

        // // /////////////////
        // // CTYPE1 - CTYPE2
        // // ////////////////
        // String cType1;
        // String cType2;
        //
        // if ("EQU".equalsIgnoreCase(coordType)) {
        // cType1 = "RA---";
        // cType2 = "DEC--";
        // } else if ("GAL".equalsIgnoreCase(coordType)) {
        // cType1 = "GLON-";
        // cType2 = "GLAT-";
        // } else if ("ECL".equalsIgnoreCase(coordType)) {
        // cType1 = "ELON-";
        // cType2 = "ELAT-";
        // } else {
        // cType1 = "UNK1-";
        // cType2 = "UNK2-";
        // }
        //
        // if (row.getString(RADESYS).toUpperCase().contains("ICRS")) {
        // cType1 = "RA---";
        // cType2 = "DEC--";
        // }
        //
        // if ("AIT".equalsIgnoreCase(coordProj) ||
        // "ARC".equalsIgnoreCase(coordProj)
        // || "CAR".equalsIgnoreCase(coordProj) ||
        // "CSC".equalsIgnoreCase(coordProj)
        // || "HPX".equalsIgnoreCase(coordProj) ||
        // "SFL".equalsIgnoreCase(coordProj)
        // || "GLS".equalsIgnoreCase(coordProj) ||
        // "SIN".equalsIgnoreCase(coordProj)
        // || "STG".equalsIgnoreCase(coordProj) ||
        // "TAN".equalsIgnoreCase(coordProj)
        // || "TOA".equalsIgnoreCase(coordProj) ||
        // "XTN".equalsIgnoreCase(coordProj)
        // || "ZEA".equalsIgnoreCase(coordProj) ||
        // "NCP".equalsIgnoreCase(coordProj)) {
        // cType1 = cType1 + coordProj;
        // cType2 = cType2 + coordProj;
        //
        // } else {
        // cType1 = cType1 + "UNK";
        // cType2 = cType2 + "UNK";
        //
        // }
        // row.set(CTYPE1, cType1);
        // row.set(CTYPE2, cType2);
        //
        // // ////////////////
        // // CRVAL1 - CRVAL2
        // // ////////////////
        //
        // double crVal1 = row.getDouble(CRVAL1);
        // double crVal2 = row.getDouble(CRVAL2);
        // double cDelt1 = row.getDouble(CDELT1);
        // double cDelt2 = row.getDouble(CDELT2);
        //
        // // crVal2 += cDelt2;
        // // crVal1 += (cDelt1 / Math.cos(crVal2 * Math.PI / 180));
        //
        row.set(CRVAL1, new Double(row.getDouble(CRVAL1) * factorConversionAngle));
        row.set(CRVAL2, new Double(row.getDouble(CRVAL2) * factorConversionAngle));
        //
        // row.set(CDELT1, new Double(0));
        // row.set(CDELT2, new Double(0));
        // // // ///////
        // // // CDELT3-> [0]
        // // // ///////
        // // Double velocity = (Double) row[keyStoreDouble.get(ALTRVAL) ] *
        // factorConversionVelocity;
        // // int channels = column1.size();
        // //
        // // // Log.getInstance().logger.debug(" *****************");
        // // // Log.getInstance().logger.debug(" * CDELT3");
        // // // Log.getInstance().logger.debug(" * channels-1: " +
        // (channels-1));
        // // //
        // //
        // Log.getInstance().logger.debug(" * (column1.get(channels-1)-column1.get(0)): "
        // // // + (column1.get(channels-1)-column1.get(0)));
        // // // Log.getInstance().logger.debug(" *****************");
        // //
        // // // Mirar CTYPE3 para diferentes opciones
        // //
        // // Double cDelt3 = new Double((column1.get(channels - 1) -
        // // column1.get(0))
        // // / (channels - 1));
        // // cDelt3 *= (1 - (velocity /
        // // Constant.SPEED_OF_LIGHT.getValue()))*factorConversionSpectral;
        // //
        // // row[keyStoreDouble.get(CDELT3)] = cDelt3 ;
        // //
        // // // ///////
        // // // CRPIX3
        // // // ///////
        // // Double crPix3 = new Double((channels - 1) / 2);
        // // row[keyStoreDouble.get(CRPIX3)] = crPix3;
        // //
        // // // Calculate observe frequencies for radial velocity in altrval
        // // // ///////
        // // // CRVAL3
        // // // ///////
        // // Double crVal3 = new Double(column1.get((int) (crPix3 - 1)));
        // //
        // // crVal3 *= (1 - (velocity / Constant.SPEED_OF_LIGHT.getValue()))*
        // // factorConversionSpectral;
        // // row[keyStoreDouble.get(CRVAL3)] = crVal3 ;
        // //
        // // // //////////////////
        // // // FREQRES (SPECRES)
        // // // //////////////////
        // // row[keyStoreDouble.get(SPECRES)] = cDelt3 ;
        //
        // // ////////
        // // ALTRVAL
        // // ////////
        // // row[keyStoreDouble.get(ALTRVAL)]; //[37]
        // // row[keyStoreDouble.get(VELOCITY)]; //[37]

        // ///////
        // VELREF
        // ///////
        int velRef = row.getInteger(VELREF);

        String velDef = AstronomicalUtilities.velDefFromVelRef(velRef);

        row.set(VELDEF, velDef);

        // String veloType = row.getString(VELOTYPE).toUpperCase();
        String specsys = row.getString(SPECSYS);
        String ctype3 = row.getString(CTYPE3).toUpperCase();

        if (!DEFAULT_SPECSYS.equals(specsys)) {
            ctype3 = ctype3.substring(0, 4) + "-" + specsys.substring(0, 3);
        } else {
            ctype3 = ctype3.substring(0, 4) + "-" + velDef.substring(5, 8);
        }

        row.set(CTYPE3, ctype3); //

        // if(!RowImport.DEFAULT_SPECSYS.equalsIgnoreCase(specsys) ||
        // !specsys.contains("SOU")){
        // ctype3 = ctype3.substring(0,4) + "-" + specsys.substring(0,3);
        // velotype = velotype.substring(0,4) + "- " + specsys.substring(0,3);
        // }else if(specsys.contains("SOU")){
        // correctionShift = true;
        // }else{
        // ctype3 = ctype3.substring(0,4) + "-" + velotype.substring(0,3);
        // }

        // ////////////////////////
        // RESTFREQ-RESTWAVE-ZSOURCE
        // ////////////////////////

        Double restFreq = row.getDouble(RESTFREQ) * factorConversionRest;

        Double restWave = row.getDouble(RESTWAVE) * factorConversionRest;
        Double imageFreq = row.getDouble(IMAGEFREQ) * factorConversionRest;
        

        row.set(RESTFREQ, restFreq);
        row.set(RESTWAVE, restWave);
        row.set(IMAGEFREQ, imageFreq);

        
        
        // String veloType = row.getString(VELOTYPE);
        double velocity = row.getDouble(ALTRVAL) * factorConversionVelocity;
        double zSource = row.getDouble(ZSOURCE);
        if(ctype3.contains("VELO"))
        {
        	velocity = row.getDouble(CRVAL3);
        	
	        if (velDef.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD)) {
	                zSource = AstronomicalUtilities.transfromRadioToRedshift(velocity);
	        } else if (velDef.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT)) {
	                zSource = AstronomicalUtilities.transfromVOptToRedshift(velocity);
	        }
        } else {

            velocity = row.getDouble(ALTRVAL) * factorConversionVelocity;
            zSource = row.getDouble(ZSOURCE);
	        if (velocity != 0) {
	            if (velDef.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD)) {
	                zSource = AstronomicalUtilities.transfromRadioToRedshift(velocity);
	            } else if (velDef.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT)) {
	                zSource = AstronomicalUtilities.transfromVOptToRedshift(velocity);
	            }
	        } else if (zSource != 0) {
	            if (velDef.toUpperCase().startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD)) {
	                velocity = AstronomicalUtilities.transfromRedshiftToRadio(zSource);
	            } else if (velDef.toUpperCase().startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT)) {
	                velocity = AstronomicalUtilities.transfromRedshiftToVOpt(zSource);
	            }
	        }
        }
        // if(zSource == 0 && velocity != 0){
        // if (veloType.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD)){
        // zSource = AstronomicalUtilities.transfromRadioToRedshift(velocity);
        // }else if (veloType.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT)){
        // zSource = AstronomicalUtilities.transfromVOptToRedshift(velocity);
        // }
        // }

        double[] values;
        // int channels = row.getInteger("NAXIS3");
        // row.set(CHANNELS, channels);

        // [Inicio correo 11/07/16]
        // SpectralTransformator specTrans;
        // if("WAVE".equalsIgnoreCase(xLabel)){
        // specTrans = new SpectralTransformator(xLabel, veloType, "", cDelt3,
        // crVal3, crPix3, Double.NaN, restWave, velocity, Double.NaN,
        // true);
        // }else{
        // specTrans = new SpectralTransformator(xLabel, veloType, "", cDelt3,
        // crVal3, crPix3, Double.NaN, restFreq, velocity, Double.NaN,
        // true);
        // }
        SpectralTransformator specTrans = null;

        // String ctype3 = row.getString(CTYPE3).substring(0,4) +
        // veloType.substring(4,8);
        // Double crVal3 = 0d;
        // double zSourceCoor = 0;

        // row.set(CTYPE3, ctype3);
        Double cDelt3 = row.getDouble(CDELT3) * factorConversionSpectral;
        Double crPix3 = row.getDouble(CRPIX3);
        Double crVal3 = row.getDouble(CRVAL3) * factorConversionSpectral;

        row.set(CDELT3, cDelt3);
        row.set(CRVAL3, crVal3); //

  //      Log.getInstance().logger.debug("ctype3="+ctype3);
        //

        if (ctype3.contains("FREQ")) 
        {
            // // ///////
            // // CDELT3-> [0]
            // // ///////
            //
            // if (veloType.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VOPT)) {
            // velocity =
            // AstronomicalUtilities.transfromRedshiftToRadio(zSource);
            // veloType = "VRAD" + veloType.substring(4, 8);
            // row.set(VELDEF, veloType);
            // velRef = new
            // Integer(AstronomicalUtilities.velRefFromVelDef(veloType));
            // row.set(VELREF, velRef);
            //
            // System.out.println("3->  " + "VRAD" + " ## " + veloType + " ## "
            // + veloType.substring(4, 8));
            //
            // }
            //
            // // Log.getInstance().logger.debug(" *****************");
            // // Log.getInstance().logger.debug(" * CDELT3");
            // // Log.getInstance().logger.debug(" * channels-1: " +
            // (channels-1));
            // //
            // Log.getInstance().logger.debug(" * (column1.get(channels-1)-column1.get(0)): "
            // // + (column1.get(channels-1)-column1.get(0)));
            // // Log.getInstance().logger.debug(" *****************");
            //
            // // Mirar CTYPE3 para diferentes opciones
            //
            // Double cDelt3 = new Double((column2.get(channels - 1) -
            // column2.get(0))
            // / (channels - 1));
            //
            // // cDelt3 = cDelt3 / (1 + zSourceCoor);
            // cDelt3 *= factorConversionSpectral;
            //
            // row.set(CDELT3, cDelt3);
            //
            // // ///////
            // // CRPIX3
            // // ///////
            // Double crPix3 = new Double((channels - 1) / 2);
            // row.set(CRPIX3, crPix3);
            //
            // // Calculate observe frequencies for radial velocity in altrval
            // // ///////
            // // CRVAL3
            // // ///////
            // crVal3 = new Double(column2.get((int) (crPix3 - 1)));
            //
            // // crVal3 = crVal3 / (1 + zSourceCoor);
            // crVal3 *= factorConversionSpectral;
            // row.set(CRVAL3, crVal3);

            // //////////////////
            // FREQRES (SPECRES)
            // //////////////////
            row.set(SPECRES, cDelt3);
            if (restFreq == 0) {
                restFreq = crVal3;
                row.set(RESTFREQ, restFreq);
            }
            specTrans = new SpectralTransformator(ctype3, velDef, "", cDelt3, crVal3, crPix3,
                    Double.NaN, restFreq, velocity, Double.NaN, true);
        } else if (ctype3.contains("WAVE")) {
            // // ///////
            // // CDELT3-> [0]
            // // ///////
            //
            // if (veloType.startsWith(MyConstants.SPECTRAL_AXIS_TYPE_VRAD)) {
            // velocity =
            // AstronomicalUtilities.transfromRedshiftToVOpt(zSource);
            // veloType = "VOPT" + veloType.substring(4, 8);
            //
            // row.set(VELDEF, veloType);
            // velRef = new
            // Integer(AstronomicalUtilities.velRefFromVelDef(veloType));
            // row.set(VELREF, velRef);
            // }
            //
            // // Log.getInstance().logger.debug(" *****************");
            // // Log.getInstance().logger.debug(" * CDELT3");
            // // Log.getInstance().logger.debug(" * channels-1: " +
            // (channels-1));
            // //
            // Log.getInstance().logger.debug(" * (column1.get(channels-1)-column1.get(0)): "
            // // + (column1.get(channels-1)-column1.get(0)));
            // // Log.getInstance().logger.debug(" *****************");
            //
            // // Mirar CTYPE3 para diferentes opciones
            //
            // Double cDelt3 = new Double((column2.get(channels - 1) -
            // column2.get(0))
            // / (channels - 1));
            // // cDelt3 *= (1 + zSourceCoor);
            // cDelt3 *= factorConversionSpectral;
            //
            // row.set(CDELT3, cDelt3);
            //
            // // ///////
            // // CRPIX3
            // // ///////
            // Double crPix3 = new Double((channels - 1) / 2);
            // row.set(CRPIX3, crPix3);
            //
            // // Calculate observe frequencies for radial velocity in altrval
            // // ///////
            // // CRVAL3
            // // ///////
            // crVal3 = new Double(column2.get((int) (crPix3 - 1)));
            //
            // // crVal3 *= (1 + zSourceCoor);
            // crVal3 *= factorConversionSpectral;
            // row.set(CRVAL3, crVal3);
            //
            // // ///////
            // // WEIGHTS
            // // ///////
            // // solo se inserta el valor al primer elementos
            // Double weight = 1 / Math.sqrt(getAverage(column3));
            //
            // column3.set(0, weight);
            //
            // row.set(WEIGHTS, column3);
            //
            // // //////////////////
            // // FREQRES (SPECRES)
            // // //////////////////
            // row.set(SPECRES, cDelt3);
            if (restWave == 0) {
                restWave = crVal3;
                row.set(RESTWAVE, restWave);
            }
            specTrans = new SpectralTransformator(ctype3, velDef, "", cDelt3, crVal3, crPix3,
                    restWave, Double.NaN, velocity, Double.NaN, true);

        } else if (ctype3.contains("VELO") ) {
        	
            // // // ///////
            // // // CDELT3-> [0]
            // // // ///////
            // //
            // // // Log.getInstance().logger.debug(" *****************");
            // // // Log.getInstance().logger.debug(" * CDELT3");
            // // // Log.getInstance().logger.debug(" * channels-1: " +
            // // (channels-1));
            // // //
            // //
            // Log.getInstance().logger.debug(" * (column1.get(channels-1)-column1.get(0)): "
            // // // + (column1.get(channels-1)-column1.get(0)));
            // // // Log.getInstance().logger.debug(" *****************");
            // //
            // // // Mirar CTYPE3 para diferentes opciones
            // //
            // Double cDelt3 = new Double((column2.get(channels - 1) -
            // column2.get(0))
            // / (channels - 1));
            // // cDelt3 *= (1 - (velocity /
            // // Constant.SPEED_OF_LIGHT.getValue()))*factorConversionSpectral;
            // //
            // row.set(CDELT3, cDelt3);
            // //
            // // // ///////
            // // // CRPIX3
            // // // ///////
            // Double crPix3 = new Double((channels - 1) / 2);
            // row.set(CRPIX3, crPix3);
            // //
            // // // Calculate observe frequencies for radial velocity in
            // altrval
            // // // ///////
            // // // CRVAL3
            // // // ///////
            // crVal3 = new Double(column2.get((int) (crPix3 - 1)));
            // //
            // // crVal3 *= (1 - (velocity /
            // Constant.SPEED_OF_LIGHT.getValue()))*
            // // factorConversionSpectral;
            // row.set(CRVAL3, crVal3);
            // //
            // // //////////////////
            // // FREQRES (SPECRES)
            // // //////////////////
            // row[keyStoreDouble.get(SPECRES)] = cDelt3 ;
            if (restFreq != 0) {
                specTrans = new SpectralTransformator(ctype3, velDef, "", cDelt3, crVal3, crPix3,
                        Double.NaN, restFreq, velocity, Double.NaN, true);
            } else if (restWave != 0) {
                specTrans = new SpectralTransformator(ctype3, velDef, "", cDelt3, crVal3, crPix3,
                        restWave, Double.NaN, velocity, Double.NaN, true);
            }

        }
        // [Fin correo 11/07/16]

        // if (veloType.contains("VRAD")) {
        // values = specTrans.getValuesFromVeloRad(velocity);
        
        //CHECKIN FOR CONSISTENCE
        values = specTrans.getValuesFromRedShift(zSource);
        // } else {
        // values = specTrans.getValuesFromVeloOpt(velocity);
        // }

        if (velDef.contains("VRAD")) {
            row.set(ALTRVAL, new Double(values[3]));
        } else if (velDef.contains("VOPT")) {
            row.set(ALTRVAL, new Double(values[4]));
        }

        row.set(ALTRPIX, new Double(values[0]));

        // ////////
        // ZSOURCE
        // ////////
        row.set(ZSOURCE, new Double(values[5]));

        // ///////////
        // TELESCOPE
        // ---------
        // BMAJ
        // BMIN
        // BPA
        // ///////////
      
        // double[] beam = WCSUtilities.calculateBminBmajFromTelescope(crVal3,
        // telescope);
        //
        // row.set(BMAJ, new Float(beam[0]));
        // row.set(BMIN, new Float(beam[1]));
        // row.set(BPA, new Float(0));
        // float bmaj = row.getFloat(BMAJ) / 3600;
        //
        // row.set(BMAJ, bmaj);
        // row.set(BMIN, bmaj);

        // ///////////
        // APEREFF
        // BEAMEFF
        // ETAFFS
        // ANTGAIN
        // ///////////

        // float[] effValues = WCSUtilities.calculateValuesFromTelescope(crVal3,
        // telescope);
        //
        // row.set(APEREFF, new Float(effValues[0]));
        // row.set(BEAMEFF, new Float(effValues[1]));
        // row.set(ETAFFS, new Float(effValues[2]));
        // row.set(ANTGAIN, new Float(effValues[3]));

        // ///////////
        // RESTFREQ
        // ///////////
        // row[keyStoreDouble.get("RESTFRQ")] = new Double();
    }

    public double calculateStdDev(double n, double sum, double sum2) {
        double stdDev;
        if (n > 0.0) {
            stdDev = (n * sum2 - sum * sum) / n;
            if (stdDev > 0.0) {
                stdDev = Math.sqrt(stdDev / (n - 1.0));
            } else {
                stdDev = 0.0;
            }
        } else {
            stdDev = 0.0;
        }


        
        return stdDev;
    }

}
