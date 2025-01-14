package es.smr.slim.plugins;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowListStarTable;
import es.cab.astronomical.AstronomicalChangeUnit;
import es.cab.astronomical.AstronomicalImagePlus;
import es.cab.astronomical.components.AstronomicalJFrameMASSA;
import es.cab.astronomical.components.AstronomicalPanelManagerMASSA;
import es.cab.astronomical.components.AstronomicalTabbedPaneMASSA;
import es.cab.astronomical.utils.AstronomicalFunctionsGenerics;
import es.cab.astronomical.utils.HistoryAndMacrosUtils;
import es.cab.madcuba.log.Log;
import es.cab.madcuba.utils.MyConstants;
import es.cab.madcuba.utils.MyUtilities;
import es.cab.plugins.WindowsPluginsUtils;
import es.cab.swing.gui.EditorInformationMADCUBA;
import es.smr.slim.DbHSQLDBCreate;
import es.smr.slim.LineSearchPanel;
import es.smr.slim.ModelLTESyntheticMoleculeFit;
import es.smr.slim.MolecularSpecies;
import es.smr.slim.components.simfit.SimFitParametersTable;
import es.smr.slim.beans.DataMolecular;
import es.smr.slim.beans.SearchSlimParams;
import es.smr.slim.calc.RangeH;
import es.smr.slim.utils.ArrayRange;
import es.smr.slim.utils.ImportFileSimulate;
import es.smr.slim.utils.Range;
import es.smr.slim.utils.SlimConstants;
import es.smr.slim.utils.SlimUtilities;
import herschel.share.unit.Frequency;
import herschel.share.unit.Unit;
import ij.IJ;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

/**
 * <p>This plugin makes a search of molecules for indicated params. 
 * The search can make for a range of indicated frequency or for a spectra. 
 * The params: </p>
 * <ul>
 * <li>range = this range is frequency list or wavelength list. The arguments can be: * 		
 * 		<ul>
 *  		<li>The list  separate with '#': freq1#freq2#..</li>
 *  		<li>selected_data: Make search about range selected frequency in associated data spectra(selected rows)/cube  </li>
 *  		<li>window_active: look the active plot from associated product and it uses the range shows in this plot</li>
 *  		<li>cube_container:(no implement) make search from range frequencies in selected cube in cube container window </li>
 * 			<li>whole: Make search about all possible frequencies (0-Double.Max). </li>		
 * 		</ul> 
 * </li>
 * <li>range = this range is frequency list or wavelength list. The arguments is separate freq1#freq2#..</li>
 * <li> axislabel = It is the label of type of unit (xAxis) showing (range). The types are:
 * 		<ul>
 *  		<li>frequency</li>
 *  		<li>wavelength</li>
 * 		</ul> 
 * </li>	 	
 * <li> axisunit = It is the label of type of selected unit.  
 * 		If the unit label is frequency then it is Hz, MHz, GHz, TGHz  
 * 		If the unit label is wavelength thin it is cm, mm, micrometer, nm, ua
 * </li>  
 * <li>molecules = It is the list of molecules in the next format:
 * 		<ul>
 * 			<li>CASE 1: CATALOG$MOLECULE$CRITERIOS#CATALOG$MOLECULE$CRITERIOS#. Ej; JPL$CS$Any$-3$Any$300#CMDS$CS+$Any$Any$Any@300#
 * 			</li>
 * 			<li>CASE 2: CATALOG$MOLECULE#. Ej1: JPL$C*#.   Ej2: lovas1992$CS#</li>
 * 			<li>CASE 3: CATALOG$MOLDECULE$DELTA#. Ej: Recomb.Lines$C$1#</li>
 * 		</ul>
 * </li>
 * <li>searchtype = the arguments are:
 * 		<ul>
 *  		<li>new:  it makes a new search in a new tab (DEFAUTL PARAMS)</li>
 *  		<li>add: the result is added to last search.</li>
 *  		<li>update_transitions: In a tab, repeat a search and add new transitions (you can change the frequency range)</li>
 * 		</ul> 
 * </li>
 * <li>datafile = It is the name of associated MADCUBA file</li>
 * <li>datatype = It is the type of associated MADCUBA file. It can be SPECTRA OR CUBE (if not exist no problem)</li>
 * <li>searchcriteria = list default values molecules,  if it not exists any molecule criteria then searched filters with this criteria (format delta_n$ElowMin$ElowMax$logI)</li>
 *<li>
 * clearsearch = It is true or false. If the parameter doesn't exist then the value is false 
 * It the parameter is true then the table is empty and the directory with all search files. Too the variables of tab used to search
 * It the parameter is false then no make nothing new. (make a search normal)
 * </li>
 * <li>sql</li>
 * <li>where</li>
 * <li>rename_molecule</li>
 * <li>inputsql</li>
 * </ul>
 * <p>Methods</p>
 * <ul>
 * <li>existsResult. Indicated it is found some transitiomn in las Search. Return true or false. Example: call("SLIM_Search.existsResult")  </li>
 * </ul>
 * 
 * @author blancoscm
 *
 */
public class SLIMSearch implements PlugIn
{
	private static final String CUBE_CONTAINER = LineSearchPanel.CUBE_CONTAINER;
	private static final String SELECTED_DATA = LineSearchPanel.SELECTED_DATA;
	private static final String SELECTED_WINDOW = LineSearchPanel.SELECTED_WINDOW;
	private static final String SELECTED_WHOLE_RANGE = LineSearchPanel.SELECTED_WHOLE_RANGE;
	private static boolean [] existsQn7InCatalogs = null;
	private static boolean isFoundNewValues = false;
	public void run(String arg) {
    	String sArg = Macro.getOptions();
    	
    	boolean isMacro = false;
		if(sArg == null || sArg.equals("")) 
		{
			sArg = arg;
		} else {
			isMacro = true;
		}
		
		if(sArg!=null &&(sArg.toLowerCase().equals("help")
				|| sArg.toLowerCase().equals("help ")))
		{
			IJ.log("\n"+this.getClass().getName()+":\n "+getInfo()+"\n");
			return;
		}
		SearchSlimParams params = getParamsPlugin(sArg);
		SLIMFRAME slimFrame = ((SLIMFRAME) WindowManager.getFrame(SlimConstants.FRAME_SLIM));// listSlectedMolecules();
	

		LineSearchPanel searchPanel = null;
		boolean isSQL = params.getSQL()!=null && !params.getSQL().isEmpty();
		isSQL = isSQL || (params.getFileSQL()!=null && !params.getFileSQL().isEmpty());

		if(!isSQL&&(params.getArrListMolecules()==null || params.getArrListMolecules().length==0))
		{
			if(!isMacro && !MyUtilities.showMessageWithNoYesOption(searchPanel, 
						"No selected molecules for searching?.\n Do you want to search all molecules?", "WARNING: Search All MOlecules"))
				return;
			else if(isMacro)
				EditorInformationMADCUBA.append("Searching all molecules");
		}

		if(slimFrame==null)
		{
			IJ.run(SlimConstants.FRAME_SLIMFRAME.replaceAll("_", " "));
			slimFrame = ((SLIMFRAME) WindowManager.getFrame(SlimConstants.FRAME_SLIM));
		} else {
			slimFrame.setVisible(true);
		}
		double memoryFree = MyUtilities.memoryDiskLinuxInfo()[0];
		if(memoryFree<0.5D)
		{
			String message = "Your computer has less than 0.5GB is posibled the application don't work correctly.";
			if (isMacro)
				EditorInformationMADCUBA.appendLineError(message);
			else if(!	MyUtilities.showMessageWithNoYesOption(null, message+"\nDo you want continue?", "Warninig Memory"))
				return;
		}
		IJ.wait(10);

		if(slimFrame!=null)
		{//TODO ESTO ESTA MAL, DEBERIA FUNCIONAR SIN PANEL SI LANZO DESDE MACRO
			searchPanel = slimFrame.getPanelLineSearch();
			if(params.getAssociatedFile()==null || params.getAssociatedFile().isEmpty())
			{
				//GET ACTIVE PRODUCTO
				String[] nameFileType = searchPanel.getNameFileMADCUBA();
				boolean isConnectTables = false;
				if(nameFileType!=null && nameFileType[0]!=null)
				{
					if (nameFileType[0]!=null)
					{
						params.setAssociatedFile(nameFileType[0]);
						
						if(slimFrame.getLineSearchResults()==null || slimFrame.getLineSearchResults().getLabelFileFITS()==null  || slimFrame.getLineSearchResults().getLabelFileFITS().isEmpty() 
								 || slimFrame.getLineSearchResults().getDataFileMASSAJ()==null
								 || slimFrame.getLineSearchResults().getDataFileMASSAJ().getNameFile()==null
								 || slimFrame.getLineSearchResults().getDataFileMASSAJ().getNameFile().isEmpty()){
							isConnectTables = true;
						}
					}

					if (nameFileType[1]!=null)
						params.setTypeAssociatedFile(nameFileType[1]);
				} 
				//MIRAR SI TIENE EN RESTO DE LOS SITIOS PARA VER SI LO CONECTO O NO, PORQUE ANTES NO ERA NECESARIO, PERO AHORA SI
				//DE HECHO HAY QUE PASAR EL NOMBRE, PARA HACERLO, SI SE PASA NOMBRE, HAY QUE COMPRARARLO CON EL DE OTRO SITIO NO CON PANES
				
//				if(params.getAssociatedFile()!=null && searchPanel.isMADCUBA_IJconnected()) {

				if(params.getAssociatedFile()!=null && isConnectTables) {
				/*	nameFileType = searchPanel.getNameMADCUBA_IJconnected();
					if (nameFileType[0]!=null)
						params.setAssociatedFile(nameFileType[0]);

					if (nameFileType[1]!=null)
						params.setTypeAssociatedFile(nameFileType[1]);*/

					//TENDRIA QUE CONECTARLO

					
					if(slimFrame.getPanelModelLTESimFit()==null)
						slimFrame.charguePanelModelLTESimFit();
					if(SLIMFRAME.getInstance(false).getPanelModelLTESimFit()!=null)
					{
						boolean isRecord = Recorder.record;
						Recorder.record = false;
						try { //ESTO NO LO DEBERIA HACER ANTES DEL SEARCH?? 
//						Log.getInstance().logger.debug("SLIM GENERATE CALL CHANGE DATA");
//							String argPlugin = "spectra='"+newFileData[0]+"' asksave=false ismacro="+isMacro;
//							IJ.getInstance().runUserPlugIn(ListPluginsNamesSlim.SLIM_CHANGE_DATA,
//										ListPluginsNamesSlim.SLIM_CHANGE_DATA, argPlugin, false);
							
//							SLIMChangeData.changeData(isMacro, false, isMacro, slimFrame, nameFileType, true,true,true); //NO CARGAR DATOS
							
						}finally {
							Recorder.record = isRecord;
						}
						
						nameFileType = SlimUtilities.changeUnitIntensityVelocity(nameFileType,false,true); //FALTARIA TODAVIA OTRA VALIDACION
					    
						if(nameFileType==null)
						{
							EditorInformationMADCUBA.appendLineError("The unit no exists. Indicated name in Modify_Data, Tab 'Operations'");
							return;
						}
						if (nameFileType[0]!=null)
							params.setAssociatedFile(nameFileType[0]);

						if (nameFileType[1]!=null)
							params.setTypeAssociatedFile(nameFileType[1]);
						searchPanel.setNameFileMADCUBA(nameFileType, true, false);
						
						SLIMFRAME.getInstance(false).getPanelModelLTESimFit().setHistorySLIM(null);
						

				//		slimFrame.getPanelModelLTESimFit().getSimFitter().setListChannelsSpectral(null);
						SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter().connectToSpectra(nameFileType);
						//AUUNQUE AQUI DEBERIA VER SI EXISTE IMPORT PARAMS
						SLIMFRAME.getInstance(false).getPanelModelLTESimFit().setTextPixel(SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter().getPixelShow(),false);
					}
				}
//				else if(isConnectTables)
//				{
//					
//				}
				if(nameFileType==null || params.getAssociatedFile()==null)
				{
					EditorInformationMADCUBA.appendLineError("No found any data (spectra or cube)");
					return;
				}
				if(slimFrame.getPanelModelLTESimFit()!=null&&slimFrame.getPanelModelLTESimFit().getSimFitter()!=null && slimFrame.getPanelModelLTESimFit().getSimFitter().tabSearchSIMFIT!=null)
	    		{
	    			if(((nameFileType[1]==null && nameFileType[0]!=null) && nameFileType[0].equals(SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW))
	    					||
	    					(nameFileType[1]!=null&&(nameFileType[1].equals(SlimConstants.FITS_TYPE_CUBE_SYNCHRO)||
	    					nameFileType[1].equals(SlimConstants.FITS_TYPE_CUBE)) )
	    					&& slimFrame.getPanelModelLTESimFit().getSimFitter().tabSearchSIMFIT.getTabCount()>=1
	    					&& params.getSearchType().equals("new"))
	    			{
	    				EditorInformationMADCUBA.appendLineError("Can't seach is a new  tab for syncronize or cube (only can be one search).");
						return;
	    			}
	    		}
			} else {
				String[] nameFileType = searchPanel.getNameFileMADCUBA();
				//MIRAR TAMBIEN DIALOG, PORQUE SE PUEDE HABER CABIADO ESTE Y TODAVIA NO TENER BUSQUEDA
				boolean isConnectTables = false;
				
				if(slimFrame.getLineSearchResults()==null || slimFrame.getLineSearchResults().getLabelFileFITS()==null  || slimFrame.getLineSearchResults().getLabelFileFITS().isEmpty() 
						 || slimFrame.getLineSearchResults().getDataFileMASSAJ()==null
						 || slimFrame.getLineSearchResults().getDataFileMASSAJ().getNameFile()==null
						 || slimFrame.getLineSearchResults().getDataFileMASSAJ().getNameFile().isEmpty()){
					isConnectTables = true;
				}
				if(nameFileType!=null&& nameFileType[0]!=null&&!isConnectTables)
				{
					if (nameFileType[0]!=null && !params.getAssociatedFile().toLowerCase().equals(nameFileType[0].toLowerCase()))
					{

						EditorInformationMADCUBA.appendLineError("The datafile different that in SLIM product. Search made in the SLIM product datafile:"+nameFileType[0]+".");

						params.setAssociatedFile(nameFileType[0]);
							
							

						if (nameFileType[1]!=null)
							params.setTypeAssociatedFile(nameFileType[1]);
					} 
	    		//						return;
					
					

			   } else {
					//MIRAMOS SI ESTA ABIERTO
					if(params.getTypeAssociatedFile()!=null && !params.getTypeAssociatedFile().isEmpty())
					{
						if(params.getTypeAssociatedFile().equals(SlimConstants.FITS_TYPE_SPECTRAL))
						{
							AstronomicalJFrameMASSA frameMASSA = (AstronomicalJFrameMASSA) WindowManager.getFrame(AstronomicalJFrameMASSA.FRAME_MASSAJ); 

							boolean existsOpenTable = false;
							if (frameMASSA != null )
							{
								//MIRO TODAS LAS PESTANAS QUE TIENE
								AstronomicalTabbedPaneMASSA tabParent= frameMASSA.getTabbed();
								AstronomicalTabbedPaneMASSA tabTable= null;
								if(tabParent!=null)					
								{									
									for (int iParent=0; iParent<tabParent.getTabCount(); iParent++)
									{					
										String sNameTab = tabParent.getTitleAt(iParent);

										if(params.getAssociatedFile()!=null &&!params.getAssociatedFile().contains(MyConstants.SEPARATOR_TABS) )
										{
											if(params.getAssociatedFile().equals(sNameTab))
											{
												existsOpenTable = true;
												tabTable = ((AstronomicalPanelManagerMASSA)tabParent.getComponentAt(iParent)).getTabbebPanel();
												String fileAssociated= params.getAssociatedFile()+MyConstants.SEPARATOR_TABS+tabTable.getTitleAt(0);
												params.setAssociatedFile(fileAssociated);
												break;	
												
											} else {
												//Les quito las terminaciones
												if(sNameTab.endsWith(".fits"))
												{
													if(!params.getAssociatedFile().endsWith(".fits"))
													{
														if((params.getAssociatedFile()+".fits").equals(sNameTab))
														{
															existsOpenTable = true;
															tabTable = ((AstronomicalPanelManagerMASSA)tabParent.getComponentAt(iParent)).getTabbebPanel();
															String fileAssociated = params.getAssociatedFile()+".fits"+MyConstants.SEPARATOR_TABS+tabTable.getTitleAt(0);
															params.setAssociatedFile(fileAssociated);
															break;	
														}
														int index = params.getAssociatedFile().lastIndexOf(".");
														if(index>0)
														{
															String fileAssociated = params.getAssociatedFile().substring(0,index);
															if((fileAssociated+".fits").equals(sNameTab))
															{
																existsOpenTable = true;
																tabTable = ((AstronomicalPanelManagerMASSA)tabParent.getComponentAt(iParent)).getTabbebPanel();
																fileAssociated= (fileAssociated+".fits")+MyConstants.SEPARATOR_TABS+tabTable.getTitleAt(0);
																params.setAssociatedFile(fileAssociated);
																break;																	
															}
														}
													}
												} else {
													int index = params.getAssociatedFile().lastIndexOf(".");
													if(index>0)
													{
														String fileAssociated = params.getAssociatedFile().substring(0,index);
														if((fileAssociated).equals(sNameTab))
														{
															existsOpenTable = true;
															tabTable = ((AstronomicalPanelManagerMASSA)tabParent.getComponentAt(iParent)).getTabbebPanel();
															fileAssociated= fileAssociated+MyConstants.SEPARATOR_TABS+tabTable.getTitleAt(0);
															params.setAssociatedFile(fileAssociated);
															break;																	
														}
													}
												}
											}
										}
										sNameTab+=MyConstants.SEPARATOR_TABS;										

										tabTable = ((AstronomicalPanelManagerMASSA)tabParent.getComponentAt(iParent)).getTabbebPanel();
										for(int iTable=0; iTable<tabTable.getTabCount(); iTable++)
										{
											String titleTable = tabTable.getTitleAt(iTable);
											
											if(params.getAssociatedFile().equals(titleTable) || params.getAssociatedFile().equals(sNameTab+titleTable) )
											{
												existsOpenTable = true;
												break;	
											}
										}
									}
								}

								if(!existsOpenTable)
								{
									EditorInformationMADCUBA.appendLineError("The datafile has not been found. Please check it.");
									return;
								}

							}
						} else if(params.getTypeAssociatedFile().equals(SlimConstants.FITS_TYPE_CUBE)) {
							List<String> namesCubes = 		WindowsPluginsUtils.getAllNameCubes();
							boolean existsOpenCube = false;
							for(String name : namesCubes)
							{
								if(name.equals(params.getAssociatedFile()) || 
										name.equals(AstronomicalImagePlus.PREFIX_CUBE+params.getAssociatedFile()) || 
										name.equals(params.getAssociatedFile().replaceFirst(AstronomicalImagePlus.PREFIX_PLOT, AstronomicalImagePlus.PREFIX_CUBE))|| 
										name.equals(params.getAssociatedFile().replaceFirst(AstronomicalImagePlus.PREFIX_PLOT, ""))|| 
										name.equals(params.getAssociatedFile().replaceFirst(AstronomicalImagePlus.PREFIX_CUBE, "")))
								{
									existsOpenCube = true;
									break;									
								}
							}
							
							if(!existsOpenCube)
							{
								EditorInformationMADCUBA.appendLineError("The datafile has not been found. Please check it.");
								return;
							}
							
						}else if(params.getTypeAssociatedFile().equals(SlimConstants.FITS_TYPE_CUBE_SYNCHRO)) {
							
						} else {
							List<String> namesCubes = 		WindowsPluginsUtils.getAllNameCubes();
							boolean existsOpenData = false;
							for(String name : namesCubes)
							{
								if(name.equals(params.getAssociatedFile()) || 
										name.equals(AstronomicalImagePlus.PREFIX_CUBE+params.getAssociatedFile()) || 
										name.equals(params.getAssociatedFile().replaceFirst(AstronomicalImagePlus.PREFIX_PLOT, AstronomicalImagePlus.PREFIX_CUBE))|| 
										name.equals(params.getAssociatedFile().replaceFirst(AstronomicalImagePlus.PREFIX_PLOT, ""))|| 
										name.equals(params.getAssociatedFile().replaceFirst(AstronomicalImagePlus.PREFIX_CUBE, "")))
								{
									existsOpenData = true;
									break;									
								}
							}
							
							if(!existsOpenData)
							{
								
								AstronomicalJFrameMASSA frameMASSA = (AstronomicalJFrameMASSA) WindowManager.getFrame(AstronomicalJFrameMASSA.FRAME_MASSAJ); 

								if (frameMASSA != null )
								{
									//MIRO TODAS LAS PESTANAS QUE TIENE
									AstronomicalTabbedPaneMASSA tabParent= frameMASSA.getTabbed();
									AstronomicalTabbedPaneMASSA tabTable= null;
									if(tabParent!=null)					
									{									
										for (int iParent=0; iParent<tabParent.getTabCount(); iParent++)
										{					
											String sNameTab = tabParent.getTitleAt(iParent);
											sNameTab+=MyConstants.SEPARATOR_TABS;										

											tabTable = ((AstronomicalPanelManagerMASSA)tabParent.getComponentAt(iParent)).getTabbebPanel();
											for(int iTable=0; iTable<tabTable.getTabCount(); iTable++)
											{
												String titleTable = tabTable.getTitleAt(iTable);
												
												if(params.getAssociatedFile().equals(titleTable) || params.getAssociatedFile().equals(sNameTab+titleTable) )
												{
													existsOpenData = true;
													
													break;
												}
											}
										}
									}

									if(!existsOpenData)
									{
										if(params.getAssociatedFile().equals(SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW) && 
												AstronomicalFunctionsGenerics.isSynchronizeCubesconnected())
										{
											existsOpenData = true;
										}
									}
								}
								
							}
							if(!existsOpenData)
							{
								EditorInformationMADCUBA.appendLineError("The datafile has not been found. Please check it.");
								return;
							}
						}
					} 

					String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};
					nameFileMAdcube = SlimUtilities.changeUnitIntensityVelocity(nameFileMAdcube,false,true); //FALTARIA TODAVIA OTRA VALIDACION
				    
					if(nameFileMAdcube==null)
					{
						EditorInformationMADCUBA.appendLineError("The unit no exists. Indicated name in Modify_Data, Tab 'Operations'");
						return;
					}
					searchPanel.setNameFileMADCUBA(nameFileMAdcube, true, false);

					if(SLIMFRAME.getInstance(false).getPanelModelLTESimFit()==null)
						SLIMFRAME.getInstance(false).charguePanelModelLTESimFit();
					if(SLIMFRAME.getInstance(false).getPanelModelLTESimFit()!=null)
					{
						SLIMFRAME.getInstance(false).getPanelModelLTESimFit().setHistorySLIM(null);
						
						
						SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter().connectToSpectra(nameFileMAdcube);
						//AUUNQUE AQUI DEBERIA VER SI EXISTE IMPORT PARAMS
						SLIMFRAME.getInstance(false).getPanelModelLTESimFit().setTextPixel(SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter().getPixelShow(),false);
					}
				}				
			}

			//PRIMEO VALIDAOS FICHERO DE DATOS (estaba en doSearch
			String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};
			if(slimFrame.getPanelModelLTESimFit()!=null&&slimFrame.getPanelModelLTESimFit().getSimFitter()!=null && slimFrame.getPanelModelLTESimFit().getSimFitter().tabSearchSIMFIT!=null)
    		{
    			if(((nameFileMAdcube[1]==null && nameFileMAdcube[0]!=null) && nameFileMAdcube[0].equals(SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW))
    					||
    					(nameFileMAdcube[1]!=null&&(nameFileMAdcube[1].equals(SlimConstants.FITS_TYPE_CUBE_SYNCHRO)||
    							nameFileMAdcube[1].equals(SlimConstants.FITS_TYPE_CUBE)) )
    					&& slimFrame.getPanelModelLTESimFit().getSimFitter().tabSearchSIMFIT.getTabCount()>=1
    					&& params.getSearchType().equals("new"))
    			{
    				EditorInformationMADCUBA.appendLineError("Can't seach is a new  tab for syncronize or cube (only can be one search).");
					return;
    			}
    		}
			
			if(!slimFrame.getPanelModelLTESimFit().changeComboUnitIntensity(nameFileMAdcube))
			{
				return;
			}
			//ADD RESULT SEARCH AHORA LO HE METIDO EN LOS IF CORESPONDIENTES; PORQUE SI NO HABIA UN CASO EN EL QUE NO ENTRABA
//			if(searchPanel.getNameFileMADCUBA()==null)
//					nameFileMAdcube = SlimUtilities.changeUnitIntensityVelocity(nameFileMAdcube,false,true);
//				
//			if(nameFileMAdcube==null)
//	 		{
//	 				EditorInformationMADCUBA.appendLineError("No search because unit no exists o is not correct. Indicated name in Modify_Data, Tab 'Operations'");
//	 				return;
//	 		}
			params.setAssociatedFile(nameFileMAdcube[0]);
			params.setTypeAssociatedFile(nameFileMAdcube[1]);
				
	 		if(!AstronomicalFunctionsGenerics.isCRType3FreqFromMADCUBA_IJ(nameFileMAdcube[0],false))
	 		{
	 				EditorInformationMADCUBA.appendLineError("No search because the data is not in Frequency. Make Resampling");
	 				return;
	 		}
//	 		SlimUtilities.validateVelocityFromMADCUBA_IJ(nameFileMAdcube[0], true);
	 		
			boolean recordTemp =Recorder.record;

			HashSet<String> moleculesUpdate= new HashSet<String>();
			HashSet<String> moleculesDelete= new HashSet<String>();

			
			isFoundNewValues = searchPlugin(isMacro, false,params, slimFrame, isSQL, nameFileMAdcube,  moleculesUpdate, moleculesDelete,true,true);
				
			if(params.getSearchType().startsWith("update"))
				slimFrame.getPanelModelLTESimFit().applySimulateAllSpecies(/*false,*/ true); //ALGO ESTA MAL POR DEBAJO. SEGURAMENTE LOS QUE NO SE HANELIMINADO SU INT. ¿cuidado entonces?¿los tengo que quitar todos?
			if(!moleculesUpdate.isEmpty())
				for(String molecule : moleculesUpdate)
				{
					EditorInformationMADCUBA.append(false,"Molecule "+molecule+" is update transitions");
				}
			if(!moleculesDelete.isEmpty())
				for(String molecule : moleculesDelete)
				{
					EditorInformationMADCUBA.append(false,"Molecule "+molecule+" is not found transitions and it is delete");
				}
				//INSERTO EL PIXEL 
			try {		
			    if(!isMacro)
			    	Recorder.record = recordTemp;
			    else
			    	Recorder.record = false;
				HistoryAndMacrosUtils.recordHistory(SLIMFRAME.getInstance(false).panelMolLTESimFit.getHistorySLIM(),HistoryAndMacrosUtils.FITS_TYPE_SLIM,this, params.getArgs(), true);
	        	Macro.setOptions(null);
			}finally{
				Recorder.record = recordTemp;
			}
		}
    }

	public static boolean searchPlugin(boolean isMacro,boolean isSameProduct, SearchSlimParams params, SLIMFRAME slimFrame,
			 boolean isSQL, String[] nameFileMAdcube,HashSet<String>  moleculesUpdate, HashSet<String>  moleculesDelete,
			 boolean isParamsCheck,boolean isWriteEditor) 
	{
		LineSearchPanel searchPanel = slimFrame.getPanelLineSearch();
		ArrayRange freqRanges = new ArrayRange();
		//MIRO SI LA BD DE DATOS TIENE O NO QN/ y en que catalogos, para que genere bien las sentencias
		//y se pueda usar la aplicación en catalogs antiguos
		if(existsQn7InCatalogs ==null)
		{
			existsQn7InCatalogs = new boolean[SlimConstants.CATALOG_LIST_NO_RECOMB.length];
			for(int i=0; i< SlimConstants.CATALOG_LIST_NO_RECOMB.length; i++)
			{  	  
				String catalogTable = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
		  	  	boolean existQN7 = validateExistsQn7(catalogTable,searchPanel.getDB());
		  	    existsQn7InCatalogs [i] = existQN7;
			}
		}
		if(!isSQL)
		{ //SI ENGO UNA SQL, REALMENTE NO TENGO QUE RECOGER EL RANGO DE FREQUENCIAS, PORQUE
			//SE SUPONE VA INCLUIDO
			ArrayList<Double> listFreq = params.getArrListRange();
			//CREATE SPECTRAL WHEN IT'S NECESSARY
			//CONVERT TFFREQ TO Hz

			if(listFreq==null||(params.getArrListRange().isEmpty() && params.getAssociatedFile()!=null && !params.getAssociatedFile().isEmpty()))
			{
				try {
					boolean isWindows = false;
					if(params.getListRange().equals(SELECTED_WINDOW))
						isWindows = true;
					listFreq  =  AstronomicalFunctionsGenerics.getListRestFrequencyMASSAJ(params.getAssociatedFile(),isWindows,true);//si no hay ninguna selececciona trae todas
//					Log.getInstance().logger.debug(listFreq);
					params.setArrListRange(listFreq);

					params.setLabelAxis("Frequency");
					params.setLabelUnitAxis("Hz");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			freqRanges = getFrequencyRangeSelected(listFreq, params.getLabelAxis(), params.getLabelUnitAxis());
		}
		boolean recordTemp = Recorder.record;

		boolean exists = false;
		try{
			Recorder.record = false;
			if(params.isClear())
			{//EMPTY VARIABLES TAB (COMO DELETE COMPONENTS) Y DIRECTORIOS
				//IF ES NEW NO TENGO QUE VACIAR NADA PORQUE ES COMO SI FUERA CLEAR.
				//O VACIO Y PONGA ADD ES OTRA OPCION

				//TEngo que vaciar todo

				searchPanel.getDialogResults().getTabResults().closeAllTab();

				if(slimFrame.getPanelModelLTESimFit()!=null && slimFrame.getPanelModelLTESimFit().sFitter!=null)
				{//VACIAR TODO LO QUE NECESITO VACIAS
					slimFrame.getPanelModelLTESimFit().sFitter.setIndexListTransition(null);
					slimFrame.getPanelModelLTESimFit().sFitter.setNullListTransition();

					slimFrame.getPanelModelLTESimFit().sFitter.emptyDataTemp();
					if(slimFrame.getPanelModelLTESimFit().sFitter.tabSearchSIMFIT !=null)
						slimFrame.getPanelModelLTESimFit().sFitter.tabSearchSIMFIT.closeAllTab() ;
				}
			}
			
			String[] listMolecules = params.getArrListMolecules();
			//SI ES SQL ESTO ES NULO (listMolecules) //A NO SER QUE HAYA OPTADO POR SACARLO 
			//DE LA SQL
			boolean isSearchRecomb = false;
			boolean isOtherSearch = false;

			ArrayList<DataMolecular> dataMolecules = new ArrayList<DataMolecular>();
			RowListStarTable criteriaSearch = null;
			HashSet< String> moleculesSearch= new HashSet<String>();
			if(!isSQL&&(listMolecules==null || listMolecules.length<1))
			{
				if(isWriteEditor)
				EditorInformationMADCUBA.appendLineError("Not exists search criteria neither molecules");
				//PUEDE QUE EN CATALOGO DE RECOMBINACION DEBE EXISTE LA OPCION DE molecules=RECPMB$*$deltaN (implementar)
				return false;
			} else {
//					Log.getInstance().logger.debug(params.getListMolecules());
				isOtherSearch =true;
				//HAY QUE DEJARLO PORQUE PUEDE VENIR DE FICHERO (AUNQUE LAS SQL NO DEBERIA PODER MEZCLARSE CON EL RESTO)
				if(!isSQL &&params.getListMolecules()!=null && params.getListMolecules().toUpperCase().contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
			    	isSearchRecomb = true;

				if(!isSQL &&listMolecules!=null)
				{//NO SE COMO LO HAREMOS CON SQL, PERO DE MOMENTO LO DEJO APARTE
					for(int i=0; i<listMolecules.length; i++)
					{
						  String[] slist =  MyUtilities.createArraysStringValues(listMolecules[i],MyConstants.SEPARATOR_NIVEL_2);
						  //LO INSERTO ANIDADO DESDE EL FICHERO, PERO NO CREO SEA LA MEJOR FORMA POR EL WHERE
						  DataMolecular dataMol = new DataMolecular();
						  dataMol.setCatalog(slist[0]);
						  dataMol.setMolecula(slist[1]);
//							  Log.getInstance().logger.debug("dataMol.getCatalog().toUpperCase()="+dataMol.getCatalog().toUpperCase());
//							  Log.getInstance().logger.debug("dataMol.SlimConstants.CATALOG_RECOMBINE.toUpperCase()().toUpperCase()="+SlimConstants.CATALOG_RECOMBINE.toUpperCase());
						  if( !dataMol.getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
						  {
							  if(slist.length>2)
							  {
								  dataMol.setElow(slist[2]);
							  } 
							  if(slist.length>3) {

								  dataMol.setCm_1(slist[3]);
							  }
							  if(slist.length>4) {

								  dataMol.setLog10(slist[4]);
							  }
							  if(slist.length>5) {

								  dataMol.setWHERE("("+slist[5]+")");
							  }
							  if(slist.length>6) {

								  dataMol.setRenameMolecule(slist[6]);
							  }
							  
							  if(slist.length==2)
							  {
								  if(params.getCriteriaDefault()!=null && !params.getCriteriaDefault().equals(""))
								  {
									  String[] criteria =  MyUtilities.createArraysStringValues(params.getCriteriaDefault(),MyConstants.SEPARATOR_NIVEL_2);
									  if(criteria.length>=4)
									  {
										  dataMol.setElow(criteria[1]);
										  dataMol.setCm_1(criteria[2]);
										  dataMol.setLog10(criteria[3]);
									  }
								  }
							  }

							  if( params.getArrListWHEREs()!=null 
									  && i<params.getArrListWHEREs().size())
								  dataMol.setWHERE("("+params.getArrListWHEREs().get(i)+")");
//							  else  if( params.getWHERE()!=null && ! params.getWHERE().isEmpty())
//								  dataMol.setWHERE("("+params.getWHERE()+")");
							  else
								  dataMol.setWHERE(params.getWHERE());
							  
							  if( params.getArrListRenameMolec()!=null 
									  && i<params.getArrListRenameMolec().size())
								  dataMol.setRenameMolecule(params.getArrListRenameMolec().get(i));
							  else if(listMolecules.length==1)
								  dataMol.setRenameMolecule(params.getNewNameMolecule()); // PERO PARA ESTO SOLO PUEDE TENER UNA MOLECULA
							  else
								  dataMol.setRenameMolecule("none");
								 
						  } else if(dataMol.getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
							  String deltaN ="Any";
							  if(slist.length>=3)
							  {
								  try{
									  new Integer(slist[2]);
									  deltaN=slist[2];
								  } catch (Exception e) {									 
								  }
							  }
							  if(deltaN.equals("Any"))
							  {
								  if(params.getCriteriaDefault()!=null && !params.getCriteriaDefault().equals(""))
								  {
									  String[] criteria =  MyUtilities.createArraysStringValues(params.getCriteriaDefault(),MyConstants.SEPARATOR_NIVEL_2);
										 
									  try{
										  if(criteria.length>=1)
										  {
											  new Integer(criteria[0]);
											  dataMol.setDelta(criteria[0]);
										  } else {
											  dataMol.setDelta("2");												  
										  }
									  } catch (Exception e) {

										  dataMol.setDelta("2");
									  }
									  
								  } else {
										  dataMol.setDelta("2");
								  }
							  } else {
								  dataMol.setDelta(deltaN);
							  }
							  
							  if(dataMol.getMolecula().length()>2)
							  {
								  if(dataMol.getMolecula().startsWith("He"))
									  dataMol.setMolecula("He");
								  else if(dataMol.getMolecula().startsWith("H"))
									  dataMol.setMolecula("H");
								  else if(dataMol.getMolecula().startsWith("C"))
									  dataMol.setMolecula("C");
								  else if(dataMol.getMolecula().startsWith("S"))
									  dataMol.setMolecula("S");
							  } 
						  }
						  if(dataMol.getRenameMolecule()!=null && !dataMol.getRenameMolecule().isEmpty()
								  && !dataMol.getRenameMolecule().toLowerCase().equals("any")
								  && !dataMol.getRenameMolecule().toLowerCase().equals("none"))
						  {
							//  moleculesSearch.add(dataMol.getCatalog().toUpperCase()+"-"+dataMol.getRenameMolecule().toUpperCase());//COMO HAGO LO DEL CATALOFO QUE NO TENGO
							  moleculesSearch.add(dataMol.getRenameMolecule().toUpperCase());//COMO HAGO LO DEL CATALOFO QUE NO TENGO
						  }
						  else
							  moleculesSearch.add(dataMol.getMolecula().toUpperCase());
						  
						  dataMolecules.add(dataMol);
					}
				} else {
					
				}			     
			    criteriaSearch = generateStarTableCriteria(dataMolecules, params, nameFileMAdcube);
			}

			if(isSQL)
			{
				exists = doSearchSQL(isSameProduct,params, searchPanel, criteriaSearch, isMacro, exists,isParamsCheck,moleculesSearch,  moleculesUpdate,  moleculesDelete);
				
			} else {//VER SI LO HAGO DESDE FICHERO, PERO EN ESE CASO
					//SOLO DEBERIA LLEVAR SQLs Y TENDRIA QUE VER COMO HACER CADA UNA
					//O LLEVAR SOLO UNA SQL
					//VER MAS ADELANTE
			    if(isSearchRecomb)
			    	exists =doSearchRecombLines(isSameProduct,params, dataMolecules, freqRanges, searchPanel,criteriaSearch,isMacro,moleculesSearch,  moleculesUpdate,  moleculesDelete,isParamsCheck);
		
//			    EditorInformationMADCUBA.append("exists="+exists);

			    if(isOtherSearch)
			    {
			    	if(params.getSearchType().toLowerCase().equals("new") && exists)
			    		params.setSearchType("add");

			    	if(isSQL)
			    		exists = doSearchSQL(isSameProduct,params,searchPanel,criteriaSearch,isMacro,exists,isParamsCheck,moleculesSearch, moleculesUpdate,  moleculesDelete);
			    	else
			    		exists = doSearch(isSameProduct,params, dataMolecules,freqRanges, searchPanel,criteriaSearch,isMacro,exists,isParamsCheck, moleculesSearch, moleculesUpdate, moleculesDelete);
			    }
			}
			if(exists)
			{
				slimFrame.setTabActived(SLIMFRAME.NAME_TAB_SIMFIT);
			}else if( isSQL){
				
				String sRenameMolecule = params.getNewNameMolecule();
				String sqlcommand = params.getSQL();
				if(sqlcommand!=null && !sqlcommand.isEmpty())
				{
					if(sRenameMolecule == null || sRenameMolecule.isEmpty())
					{
						//ME TENGO QUE TRAER EL DE LA SENTENCIA
						//CUIDADO CONEL none, que puede venir en rename='none'
						int indexCata = sqlcommand.indexOf("cata");
						int indexRename =  sqlcommand.indexOf("id_rename");
						int indexnone =  sqlcommand.indexOf("none");
						 if(indexnone<0 && indexRename>0 && indexCata>0){
							sRenameMolecule = sqlcommand.substring(indexCata+6, indexRename-5);
						}
					} else if(sRenameMolecule.toLowerCase().equals("none")) {
		          		int indexFormula = sqlcommand.indexOf("id_formula=");
//		          		 Log.getInstance().logger.debug("-A---indexFormula"+indexFormula);
		          		sRenameMolecule = "";
		          		
						if(indexFormula>0)
						{
							sRenameMolecule = sqlcommand.substring(indexFormula+12);

//			          		 Log.getInstance().logger.debug("--A--VER QUE FORMULA COJE"+sForm);
							indexFormula =sRenameMolecule.indexOf("'");
							if(indexFormula<0)
								indexFormula =sRenameMolecule.indexOf("$");
//			          		 Log.getInstance().logger.debug("--B--VER QUE indexFormula COJE"+indexFormula);

							sRenameMolecule = sRenameMolecule.substring(0,indexFormula);
						} 
					}
				}
				if(sRenameMolecule == null || sRenameMolecule.isEmpty())
					EditorInformationMADCUBA.append(false,"Not found new transitions for indicated SQL");
				else
					EditorInformationMADCUBA.append(false,"Not found new transitions for indicated '"+sRenameMolecule+"'");
			}
			
			//INDICATED LAS QUE NO SE HAN REALIZADO LA CONSULTA
			if(!isSameProduct&&isWriteEditor)
			{
				if(moleculesUpdate==null  && moleculesDelete==null)
				{
					for(String molecule : moleculesSearch)
					{
						if(!molecule.contains("*"))
							EditorInformationMADCUBA.append(false,"Not found new transitions for "+molecule);
					}
				} else if(moleculesUpdate!=null  && moleculesDelete!=null){
					for(String molecule : moleculesSearch)
					{
						boolean containsDelete = false;
						for(String moleculeD : moleculesDelete)
						{
							if(moleculeD.toUpperCase().endsWith("-"+molecule.toUpperCase()))
							{
								containsDelete= true;
								break;
							}
						}
						boolean containsUpdate = false;
						for(String moleculeU : moleculesUpdate)
						{
							if(moleculeU.toUpperCase().endsWith("-"+molecule.toUpperCase()))
							{
								containsUpdate= true;
								break;
							}
						}
						if(!molecule.contains("*") && !containsDelete && !containsUpdate)
							EditorInformationMADCUBA.append(false,"Not found new transitions for "+molecule);
					}
				}else if(moleculesUpdate!=null ){
					for(String molecule : moleculesSearch)
					{
						boolean containsUpdate = false;
						for(String moleculeU : moleculesUpdate)
						{
							if(moleculeU.toUpperCase().endsWith("-"+molecule.toUpperCase()))
							{
								containsUpdate= true;
								break;
							}
						}
						if(!molecule.contains("*")&& !containsUpdate)
							EditorInformationMADCUBA.append(false,"Not found new transitions for "+molecule);
					}
				}else if(moleculesDelete!=null ){
					for(String molecule : moleculesSearch)
					{
						boolean containsDelete = false;
						for(String moleculeD : moleculesDelete)
						{
							if(moleculeD.toUpperCase().endsWith("-"+molecule.toUpperCase()))
							{
								containsDelete= true;
								break;
							}
						}
						if(!molecule.contains("*")&&!containsDelete)
							EditorInformationMADCUBA.append(false,"Not found new transitions for "+molecule);
					}
				}
			}
		}finally{
				Recorder.record = recordTemp;
		}
		return exists;
	}
	
	public static MolecularSpecies searchMoleculeSpecies(boolean isMacro, SearchSlimParams params, SLIMFRAME slimFrame,
			 boolean isSQL, String[] nameFileMAdcube, boolean isParamsCheck,boolean isWriteEditor) 
	{
		LineSearchPanel searchPanel = slimFrame.getPanelLineSearch();
		MolecularSpecies molecularSpecies = null;
		ArrayRange freqRanges = new ArrayRange();
		//MIRO SI LA BD DE DATOS TIENE O NO QN/ y en que catalogos, para que genere bien las sentencias
		//y se pueda usar la aplicación en catalogs antiguos
		if(existsQn7InCatalogs ==null)
		{
			existsQn7InCatalogs = new boolean[SlimConstants.CATALOG_LIST_NO_RECOMB.length];
			for(int i=0; i< SlimConstants.CATALOG_LIST_NO_RECOMB.length; i++)
			{  	  
				String catalogTable = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
		  	  	boolean existQN7 = validateExistsQn7(catalogTable,searchPanel.getDB());
		  	    existsQn7InCatalogs [i] = existQN7;
			}
		}
		if(!isSQL)
		{ //SI ENGO UNA SQL, REALMENTE NO TENGO QUE RECOGER EL RANGO DE FREQUENCIAS, PORQUE
			//SE SUPONE VA INCLUIDO
			ArrayList<Double> listFreq = params.getArrListRange();
			//CREATE SPECTRAL WHEN IT'S NECESSARY
			//CONVERT TFFREQ TO Hz
			if(params.getListRange()!=null && 	params.getListRange().toLowerCase().equals(SELECTED_WHOLE_RANGE))
			{
				ArrayList<Double> rangeList = new ArrayList<Double>();
				rangeList.add(0D);
				rangeList.add(Double.MAX_VALUE);//OR 300000 UN PAR DE CEROS MAS
				params.setArrListRange(rangeList);
				listFreq = params.getArrListRange();
				params.setLabelAxis("Frequency");
				params.setLabelUnitAxis("Hz");
//				isWholeRange = true;
			} else if(listFreq==null||(params.getArrListRange().isEmpty() && params.getAssociatedFile()!=null && !params.getAssociatedFile().isEmpty()))
			{
				try {
					boolean isWindows = false;
					if(params.getListRange().equals(SELECTED_WINDOW))
						isWindows = true;
					listFreq  =  AstronomicalFunctionsGenerics.getListRestFrequencyMASSAJ(params.getAssociatedFile(),isWindows,true);//si no hay ninguna selececciona trae todas
//					Log.getInstance().logger.debug(listFreq);
					params.setArrListRange(listFreq);

					params.setLabelAxis("Frequency");
					params.setLabelUnitAxis("Hz");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			freqRanges = getFrequencyRangeSelected(listFreq, params.getLabelAxis(), params.getLabelUnitAxis());
		}
		boolean recordTemp = Recorder.record;

		boolean exists = false;
		try{
			Recorder.record = false;
			
			String[] listMolecules = params.getArrListMolecules();
			//SI ES SQL ESTO ES NULO (listMolecules) //A NO SER QUE HAYA OPTADO POR SACARLO 
			//DE LA SQL
			boolean isSearchRecomb = false;
			boolean isOtherSearch = false;

			ArrayList<DataMolecular> dataMolecules = new ArrayList<DataMolecular>();
//			RowListStarTable criteriaSearch = null;
			HashSet< String> moleculesSearch= new HashSet<String>();
			if(!isSQL&&(listMolecules==null || listMolecules.length<1))
			{
				if(isWriteEditor)
				EditorInformationMADCUBA.appendLineError("Not exists search criteria neither molecules");
				//PUEDE QUE EN CATALOGO DE RECOMBINACION DEBE EXISTE LA OPCION DE molecules=RECPMB$*$deltaN (implementar)
				return null;
			} else {
//					Log.getInstance().logger.debug(params.getListMolecules());
				isOtherSearch =true;
				//HAY QUE DEJARLO PORQUE PUEDE VENIR DE FICHERO (AUNQUE LAS SQL NO DEBERIA PODER MEZCLARSE CON EL RESTO)
				if(!isSQL &&params.getListMolecules()!=null && params.getListMolecules().toUpperCase().contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
			    	isSearchRecomb = true;

				if(!isSQL &&listMolecules!=null)
				{//NO SE COMO LO HAREMOS CON SQL, PERO DE MOMENTO LO DEJO APARTE
					for(int i=0; i<listMolecules.length; i++)
					{
						  String[] slist =  MyUtilities.createArraysStringValues(listMolecules[i],MyConstants.SEPARATOR_NIVEL_2);
						  //LO INSERTO ANIDADO DESDE EL FICHERO, PERO NO CREO SEA LA MEJOR FORMA POR EL WHERE
						  DataMolecular dataMol = new DataMolecular();
						  dataMol.setCatalog(slist[0]);
						  dataMol.setMolecula(slist[1]);
//							  Log.getInstance().logger.debug("dataMol.getCatalog().toUpperCase()="+dataMol.getCatalog().toUpperCase());
//							  Log.getInstance().logger.debug("dataMol.SlimConstants.CATALOG_RECOMBINE.toUpperCase()().toUpperCase()="+SlimConstants.CATALOG_RECOMBINE.toUpperCase());
						  if( !dataMol.getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
						  {
							  if(slist.length>2)
							  {
								  dataMol.setElow(slist[2]);
							  } 
							  if(slist.length>3) {

								  dataMol.setCm_1(slist[3]);
							  }
							  if(slist.length>4) {

								  dataMol.setLog10(slist[4]);
							  }
							  if(slist.length>5) {

								  dataMol.setWHERE(slist[5]);
							  }
							  if(slist.length>6) {

								  dataMol.setRenameMolecule(slist[6]);
							  }
							  
							  if(slist.length==2)
							  {
								  if(params.getCriteriaDefault()!=null && !params.getCriteriaDefault().equals(""))
								  {
									  String[] criteria =  MyUtilities.createArraysStringValues(params.getCriteriaDefault(),MyConstants.SEPARATOR_NIVEL_2);
									  if(criteria.length>=4)
									  {
										  dataMol.setElow(criteria[1]);
										  dataMol.setCm_1(criteria[2]);
										  dataMol.setLog10(criteria[3]);
									  }
								  }
							  }

							  if( params.getArrListWHEREs()!=null 
									  && i<params.getArrListWHEREs().size())
								  dataMol.setWHERE(params.getArrListWHEREs().get(i));
							  else
								  dataMol.setWHERE(params.getWHERE());
							  
							  if( params.getArrListRenameMolec()!=null 
									  && i<params.getArrListRenameMolec().size())
								  dataMol.setRenameMolecule(params.getArrListRenameMolec().get(i));
							  else if(listMolecules.length==1)
								  dataMol.setRenameMolecule(params.getNewNameMolecule()); // PERO PARA ESTO SOLO PUEDE TENER UNA MOLECULA
							  else
								  dataMol.setRenameMolecule("none");
								 
						  } else if(dataMol.getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
							  String deltaN ="Any";
							  if(slist.length>=3)
							  {
								  try{
									  new Integer(slist[2]);
									  deltaN=slist[2];
								  } catch (Exception e) {									 
								  }
							  }
							  if(deltaN.equals("Any"))
							  {
								  if(params.getCriteriaDefault()!=null && !params.getCriteriaDefault().equals(""))
								  {
									  String[] criteria =  MyUtilities.createArraysStringValues(params.getCriteriaDefault(),MyConstants.SEPARATOR_NIVEL_2);
										 
									  try{
										  if(criteria.length>=1)
										  {
											  new Integer(criteria[0]);
											  dataMol.setDelta(criteria[0]);
										  } else {
											  dataMol.setDelta("2");												  
										  }
									  } catch (Exception e) {

										  dataMol.setDelta("2");
									  }
									  
								  } else {
										  dataMol.setDelta("2");
								  }
							  } else {
								  dataMol.setDelta(deltaN);
							  }
							  
							  if(dataMol.getMolecula().length()>2)
							  {
								  if(dataMol.getMolecula().startsWith("He"))
									  dataMol.setMolecula("He");
								  else if(dataMol.getMolecula().startsWith("H"))
									  dataMol.setMolecula("H");
								  else if(dataMol.getMolecula().startsWith("C"))
									  dataMol.setMolecula("C");
								  else if(dataMol.getMolecula().startsWith("S"))
									  dataMol.setMolecula("S");
							  } 
						  }
						  if(dataMol.getRenameMolecule()!=null && !dataMol.getRenameMolecule().isEmpty()
								  && !dataMol.getRenameMolecule().toLowerCase().equals("any")
								  && !dataMol.getRenameMolecule().toLowerCase().equals("none"))
							  moleculesSearch.add(dataMol.getRenameMolecule().toUpperCase());
						  else
							  moleculesSearch.add(dataMol.getMolecula().toUpperCase());
						  
						  dataMolecules.add(dataMol);
					}
				} else {
					
				}			     
//			    criteriaSearch = generateStarTableCriteria(dataMolecules, params, nameFileMAdcube);
			}
			if(isSQL)
			{
				
				molecularSpecies = doSearchMoleculaSpeciesSQL(false,params, searchPanel,  isMacro, exists,isParamsCheck,moleculesSearch);
				if(molecularSpecies !=null)
					return molecularSpecies;
			} else {//VER SI LO HAGO DESDE FICHERO, PERO EN ESE CASO
					//SOLO DEBERIA LLEVAR SQLs Y TENDRIA QUE VER COMO HACER CADA UNA
					//O LLEVAR SOLO UNA SQL
					//VER MAS ADELANTE
			    if(isSearchRecomb)
			    	return null; //PORQUE DE MOMENTO SOLO HAY UNA TRANSICION POR UNA MOLECULA, Y HABRIA QUE MIRAR COMO SE RECALCULARexists =doSearchRecombLines(isSameProduct,params, dataMolecules, freqRanges, searchPanel,criteriaSearch,isMacro,moleculesSearch,  moleculesUpdate,  moleculesDelete,isParamsCheck);
		
//			    EditorInformationMADCUBA.append("exists="+exists);

			    if(isOtherSearch)
			    {
			    

			    	if(isSQL)
			    		molecularSpecies = doSearchMoleculaSpeciesSQL(false,params,searchPanel,isMacro,exists,isParamsCheck,moleculesSearch);
			    	else
			    		molecularSpecies = doSearchMolecularSpecies(false,params, dataMolecules,freqRanges, searchPanel,isMacro,exists,isParamsCheck, moleculesSearch);
			    	
//			    	 doSearchMolecularSpecies(boolean isSameProduct,SearchSlimParams params, ArrayList<DataMolecular> dataMoleculesIni, ArrayRange freqRanges, 
//			    				LineSearchPanel searchPanel, boolean isMacro, boolean exists, boolean isParamsCheck,
//			    				HashSet<String> moleculesSearch) 
			    }
			}
		}finally{
				Recorder.record = recordTemp;
		}
		return molecularSpecies;
	}
	private static boolean doSearchSQL(boolean isSameProduct, SearchSlimParams params, LineSearchPanel searchPanel, RowListStarTable criteriaSearch,
			boolean isMacro, boolean exists,boolean isParamsCheck, HashSet<String> moleculesSearch,  HashSet<String> moleculesUpdate, HashSet<String> moleculesDelete) 
	{			
		if(!searchPanel.getDialogResults().isVisible())
			searchPanel.getDialogResults().setVisible(true);	
//		searchPanel.getDialogResults().toFront();
		System.gc();
		String sqlcommand;
		Boolean isResult = false;
		if((params.getSQL()==null || params.getSQL().isEmpty() || params.getSQL().toLowerCase().equals("any"))
				&&(params.getFileSQL()==null || params.getFileSQL().isEmpty() || params.getArrListSQLs()==null || params.getArrListSQLs().isEmpty())
				)
			return exists; //NO PUEDO RELLENARLO PORQUE ES NULO
		ArrayList<ResultSet> dbResultsArray = new ArrayList<ResultSet>();
		ArrayList<String> newsqlcommand =  null;
		//RECUERDA POSIBLEMENTE NECESITE SACAR INFORMACION DEL WHERE
		/*
		 * First gets the name of the table in DB corresponding to the Long Name selected in the Catalog Combo Box
		 */
//		sqlcommand = "SELECT id_tablename FROM TableIndex "+
//						"WHERE id_tablelongname = '".concat(catalogFin).concat("'");
//
//		ResultSet dbresult = searchPanel.getDB().query(sqlcommand);
//		String row = catalogFin;//params.getCatalog();
//		if(dbresult.next())
//        	row = dbresult.getObject(1).toString();
		//ESTO TIENE QUE VENIR DE LA SQL (VEr QUE NECESITO)
		
		//AQUI DENTRO CONSIDERO LOS WHERE
   //     newsqlcommand = generateSQLcommand(row, params, dataMolecules/*, searchPanel.getDialogResults()*/);
		newsqlcommand = new ArrayList<String>();
//		if(dataMoleculesIni==null || dataMoleculesIni.isEmpty())
//		{
		ArrayList<String> listSQL = params.getArrListSQLs();
		ArrayList<String> listRename = params.getArrListRenameMolec();

		if(listSQL==null || listSQL.isEmpty())
		{
			listSQL = new ArrayList<String>();
			listSQL.add(params.getSQL());
			listRename = new ArrayList<String>();
			listRename.add(params.getNewNameMolecule());
		}
			//COMO LOS ' no puedo pasarlo porque se lia hay que cambiar el lo a algo que no vaya a 
			//usar en SQL como @ p # 
			//voy a poner $ pero creo que esto se puede usar
		for(int i=0; i<listSQL.size(); i++)
		{
			sqlcommand = listSQL.get(i);
			
			sqlcommand = sqlcommand.replace('$', '\'');
			//ME FALTA LO DEL NAME DE LA CONSULTA
			String sRenameMolecule = listRename.get(i);

			if(sRenameMolecule == null || sRenameMolecule.isEmpty())
			{
				//ME TENGO QUE TRAER EL DE LA SENTENCIA
				//CUIDADO CONEL none, que puede venir en rename='none'
				int indexCata = sqlcommand.indexOf("cata");
				int indexRename =  sqlcommand.indexOf("id_rename");
				int indexnone =  sqlcommand.indexOf("none");
				if(indexRename<0)
				{
					String sqlRename = "cata, 'none' as id_rename";
					sqlcommand = sqlcommand.replaceFirst("cata",sqlRename);
				}else if(indexnone<0 && indexRename>0 && indexCata>0){
					sRenameMolecule = sqlcommand.substring(indexCata+6, indexRename-5);
				}
			} else if(sRenameMolecule.toLowerCase().equals("none")){
				int indexCata = sqlcommand.indexOf("cata");
				int indexRename =  sqlcommand.indexOf("id_rename");
				int indexnone =  sqlcommand.indexOf("none");
				if(indexRename<0)
				{
					String sqlRename = "cata, 'none' as id_rename";
					sqlcommand = sqlcommand.replaceFirst("cata",sqlRename);
				}else if(indexnone<0 && indexRename>0 && indexCata>0){
					String sqlRename = sqlcommand.substring(0,indexCata+5);
					sqlRename += "'none' as id_rename";
					sqlRename += sqlcommand.substring(indexRename+9);
					sqlcommand = sqlRename;
				}
				
			} else {
				//CAMBIO EL VALOR DE LA SQL
//				cata,"+
//      			"'none' as id_rename")
				int indexCata = sqlcommand.indexOf("cata");
				int indexRename =  sqlcommand.indexOf("id_rename");
				int indexnone =  sqlcommand.indexOf("none");
				if(indexRename<0)
				{
					String sqlRename = "cata, '"+sRenameMolecule+"' as id_rename";
					sqlcommand = sqlcommand.replaceFirst("cata",sqlRename);
				} else if(indexnone>0)
				{
					sqlcommand = sqlcommand.replaceFirst("none",sRenameMolecule);
				} else if(indexRename>0 && indexCata>0){
					String sqlRename = sqlcommand.substring(0,indexCata+5);
					sqlRename += "'"+sRenameMolecule+"' as id_rename";
					sqlRename += sqlcommand.substring(indexRename+9);
					sqlcommand = sqlRename;
				}
			}

		  	ArrayList<Integer> resultSearch = null; 
		  	ModelLTESyntheticMoleculeFit panelModel = null;
			SLIMFRAME slimFrame = ((SLIMFRAME) WindowManager.getFrame(SlimConstants.FRAME_SLIM));
			if(slimFrame!=null)
			{
				if(slimFrame.getPanelModelLTESimFit()==null)
					slimFrame.charguePanelModelLTESimFit();
				panelModel = slimFrame.getPanelModelLTESimFit();
			} 
			int iColumnForm = -1;
			if(panelModel!=null)
				iColumnForm = SimFitParametersTable.columnFormula;
			if(iColumnForm>-1)
			{
				if(sRenameMolecule.toLowerCase().startsWith("rem_"))
				{
					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
	
		          	  if(resultSearch==null || resultSearch.size()<1)
		          	  {
		          		  String sForm = sRenameMolecule.substring(4);
		          		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);
					  } 
				}else if(sRenameMolecule.toLowerCase().equals("none"))
				{
//					id_formula='t-C4H9CN'					 
	          		String sForm = sRenameMolecule.substring(4);
	          		int indexFormula = sqlcommand.indexOf("id_formula=");
					if(indexFormula>0)
					{
						sForm = sqlcommand.substring(indexFormula+12);

						indexFormula =sForm.indexOf("'");

						sForm = sForm.substring(0,indexFormula);
					} 
					if(sForm.indexOf("LIKE")<0 && sForm.indexOf("*")<0)
	          		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);
		
				}else {
					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
				}
			}
		  	if(params.getSearchType().equals("new") || resultSearch==null || resultSearch.size()<1)
		  	{
		  		  newsqlcommand.add(sqlcommand);
		  	}			
		}
		//DE MOMENTO SOLO PARA JPL,CDMS,USER
		//MAS ADELANTE VERLO PARA LOVAS
		try
		{				
			int iCount = 1;
	        int iFinal = newsqlcommand.size();
	        for (String sql : newsqlcommand)
	        {
	    		IJ.showProgress(iCount/iFinal);
	    		IJ.showStatus("MAKING QUERY/S:"+ iCount+" / "+iFinal);	
//	    		Log.getInstance().logger.debug("---:"+sql);
	        	dbResultsArray = getFrequenciesAndQuery(sql,dbResultsArray,searchPanel.getDB(),null);
	        	iCount++;
	        }
	          
	            
	            //TODO DEBE DECIR SI NUEVA O EXISTENTE
	            
	            //ANTES DE BUSCAR TIENE QUE INDICAR SI EL PRODUCTO NOS VALE O NO
	        String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};
	        
			isResult = searchPanel.getDialogResults().readSearchResult(isSameProduct,criteriaSearch,
	         			dbResultsArray, /*catalogFin.trim(), */
	         			params.getSearchType(), nameFileMAdcube,isMacro, exists, isParamsCheck,
	         			moleculesSearch,   moleculesUpdate,  moleculesDelete); //LA VOY APASAR VACIA,
			//PERO SEGURO QUE TENDRIA QUE VENIR DE LA CONSULTA
			if(isResult && !exists&&!isSameProduct)
			{
				   //CUIDADO AL BUSCAR, CUANDO SE ADD LOS PARAMS DEL RESULTADO
				   //NO DEBE CAMBIAR FILTER NI NOISE DEL RESTO, PERO SI DEL QUE HE ADD
				   //EN LA BUSQUEDA
	//			  SlimUtilities.validateRMSFromSlim(nameFileMAdcube[0]);	
				   searchPanel.setNameFileMADCUBA(nameFileMAdcube, true, false);
				   //RMS SOLO SI YA NO SE HA PUESTO	        	
			}
		 
			for(ResultSet resultdata: dbResultsArray)
				if(resultdata!=null)
					resultdata.close();
			IJ.showProgress(1);
	    } catch (SQLException sqlException) {
	    	Log.getInstance().logger.error(sqlException.getMessage());
	        
	    }	 finally {
	    	dbResultsArray.clear();
	    	if(newsqlcommand !=null )
	    		newsqlcommand.clear();
	    	params =null;
	    	newsqlcommand=null;
	    }
		return isResult || exists;
	}
	
	private static MolecularSpecies doSearchMoleculaSpeciesSQL(boolean isSameProduct, SearchSlimParams params, LineSearchPanel searchPanel,	boolean isMacro, boolean exists,boolean isParamsCheck, HashSet<String> moleculesSearch) 
	{			
		if(!searchPanel.getDialogResults().isVisible())
			searchPanel.getDialogResults().setVisible(true);	
//		searchPanel.getDialogResults().toFront();
		System.gc();
		String sqlcommand;
		Boolean isResult = false;
		if((params.getSQL()==null || params.getSQL().isEmpty() || params.getSQL().toLowerCase().equals("any"))
				&&(params.getFileSQL()==null || params.getFileSQL().isEmpty() || params.getArrListSQLs()==null || params.getArrListSQLs().isEmpty())
				)
			return null; //NO PUEDO RELLENARLO PORQUE ES NULO
		ArrayList<ResultSet> dbResultsArray = new ArrayList<ResultSet>();
		ArrayList<String> newsqlcommand =  null;
		//RECUERDA POSIBLEMENTE NECESITE SACAR INFORMACION DEL WHERE
		/*
		 * First gets the name of the table in DB corresponding to the Long Name selected in the Catalog Combo Box
		 */
//		sqlcommand = "SELECT id_tablename FROM TableIndex "+
//						"WHERE id_tablelongname = '".concat(catalogFin).concat("'");
//
//		ResultSet dbresult = searchPanel.getDB().query(sqlcommand);
//		String row = catalogFin;//params.getCatalog();
//		if(dbresult.next())
//        	row = dbresult.getObject(1).toString();
		//ESTO TIENE QUE VENIR DE LA SQL (VEr QUE NECESITO)
		
		//AQUI DENTRO CONSIDERO LOS WHERE
   //     newsqlcommand = generateSQLcommand(row, params, dataMolecules/*, searchPanel.getDialogResults()*/);
		newsqlcommand = new ArrayList<String>();
//		if(dataMoleculesIni==null || dataMoleculesIni.isEmpty())
//		{
		ArrayList<String> listSQL = params.getArrListSQLs();
		ArrayList<String> listRename = params.getArrListRenameMolec();

		if(listSQL==null || listSQL.isEmpty())
		{
			listSQL = new ArrayList<String>();
			listSQL.add(params.getSQL());
			listRename = new ArrayList<String>();
			listRename.add(params.getNewNameMolecule());
		}
			//COMO LOS ' no puedo pasarlo porque se lia hay que cambiar el lo a algo que no vaya a 
			//usar en SQL como @ p # 
			//voy a poner $ pero creo que esto se puede usar
		for(int i=0; i<listSQL.size(); i++)
		{
			sqlcommand = listSQL.get(i);
			
			sqlcommand = sqlcommand.replace('$', '\'');
			//ME FALTA LO DEL NAME DE LA CONSULTA
			String sRenameMolecule = listRename.get(i);

			if(sRenameMolecule == null || sRenameMolecule.isEmpty())
			{
				//ME TENGO QUE TRAER EL DE LA SENTENCIA
				//CUIDADO CONEL none, que puede venir en rename='none'
				int indexCata = sqlcommand.indexOf("cata");
				int indexRename =  sqlcommand.indexOf("id_rename");
				int indexnone =  sqlcommand.indexOf("none");
				if(indexRename<0)
				{
					String sqlRename = "cata, 'none' as id_rename";
					sqlcommand = sqlcommand.replaceFirst("cata",sqlRename);
				}else if(indexnone<0 && indexRename>0 && indexCata>0){
					sRenameMolecule = sqlcommand.substring(indexCata+6, indexRename-5);
				}
			} else if(sRenameMolecule.toLowerCase().equals("none")){
				int indexCata = sqlcommand.indexOf("cata");
				int indexRename =  sqlcommand.indexOf("id_rename");
				int indexnone =  sqlcommand.indexOf("none");
				if(indexRename<0)
				{
					String sqlRename = "cata, 'none' as id_rename";
					sqlcommand = sqlcommand.replaceFirst("cata",sqlRename);
				}else if(indexnone<0 && indexRename>0 && indexCata>0){
					String sqlRename = sqlcommand.substring(0,indexCata+5);
					sqlRename += "'none' as id_rename";
					sqlRename += sqlcommand.substring(indexRename+9);
					sqlcommand = sqlRename;
				}
				
			} else {
				//CAMBIO EL VALOR DE LA SQL
//				cata,"+
//      			"'none' as id_rename")
				int indexCata = sqlcommand.indexOf("cata");
				int indexRename =  sqlcommand.indexOf("id_rename");
				int indexnone =  sqlcommand.indexOf("none");
				if(indexRename<0)
				{
					String sqlRename = "cata, '"+sRenameMolecule+"' as id_rename";
					sqlcommand = sqlcommand.replaceFirst("cata",sqlRename);
				} else if(indexnone>0)
				{
					sqlcommand = sqlcommand.replaceFirst("none",sRenameMolecule);
				} else if(indexRename>0 && indexCata>0){
					String sqlRename = sqlcommand.substring(0,indexCata+5);
					sqlRename += "'"+sRenameMolecule+"' as id_rename";
					sqlRename += sqlcommand.substring(indexRename+9);
					sqlcommand = sqlRename;
				}
			}

		  	ArrayList<Integer> resultSearch = null; 
		  	ModelLTESyntheticMoleculeFit panelModel = null;
			SLIMFRAME slimFrame = ((SLIMFRAME) WindowManager.getFrame(SlimConstants.FRAME_SLIM));
			if(slimFrame!=null)
			{
				if(slimFrame.getPanelModelLTESimFit()==null)
					slimFrame.charguePanelModelLTESimFit();
				panelModel = slimFrame.getPanelModelLTESimFit();
			} 
			int iColumnForm = -1;
			if(panelModel!=null)
				iColumnForm = SimFitParametersTable.columnFormula;
			if(iColumnForm>-1)
			{
				if(sRenameMolecule.toLowerCase().startsWith("rem_"))
				{
					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
	
		          	  if(resultSearch==null || resultSearch.size()<1)
		          	  {
		          		  String sForm = sRenameMolecule.substring(4);
		          		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);
					  } 
				}else if(sRenameMolecule.toLowerCase().equals("none"))
				{
//					id_formula='t-C4H9CN'					 
	          		String sForm = sRenameMolecule.substring(4);
	          		int indexFormula = sqlcommand.indexOf("id_formula=");
					if(indexFormula>0)
					{
						sForm = sqlcommand.substring(indexFormula+12);

						indexFormula =sForm.indexOf("'");

						sForm = sForm.substring(0,indexFormula);
					} 
					if(sForm.indexOf("LIKE")<0 && sForm.indexOf("*")<0)
	          		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);
		
				}else {
					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
				}
			}
		  	if(params.getSearchType().equals("new") || resultSearch==null || resultSearch.size()<1)
		  	{
		  		  newsqlcommand.add(sqlcommand);
		  	}			
		}
		//DE MOMENTO SOLO PARA JPL,CDMS,USER
		//MAS ADELANTE VERLO PARA LOVAS
		MolecularSpecies molecSpecies = null;
		try
		{				
			int iCount = 1;
	        int iFinal = newsqlcommand.size();
	        for (String sql : newsqlcommand)
	        {
	    		IJ.showProgress(iCount/iFinal);
	    		IJ.showStatus("MAKING QUERY/S:"+ iCount+" / "+iFinal);	
//	    		Log.getInstance().logger.debug("---:"+sql);
	        	dbResultsArray = getFrequenciesAndQuery(sql,dbResultsArray,searchPanel.getDB(),null);
	        	iCount++;
	        }
	          
	            
	            //TODO DEBE DECIR SI NUEVA O EXISTENTE
	            
	            //ANTES DE BUSCAR TIENE QUE INDICAR SI EL PRODUCTO NOS VALE O NO
	        String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};

	        molecSpecies = searchPanel.getDialogResults().readSearchResult(
	         			dbResultsArray, 
	         			nameFileMAdcube,isMacro, exists, isParamsCheck); //LA VOY APASAR VACIA,
			//PERO SEGURO QUE TENDRIA QUE VENIR DE LA CONSULTA
//			if(isResult && !exists&&!isSameProduct)
//			{
//				   //CUIDADO AL BUSCAR, CUANDO SE ADD LOS PARAMS DEL RESULTADO
//				   //NO DEBE CAMBIAR FILTER NI NOISE DEL RESTO, PERO SI DEL QUE HE ADD
//				   //EN LA BUSQUEDA
//	//			  SlimUtilities.validateRMSFromSlim(nameFileMAdcube[0]);	
//				   searchPanel.setNameFileMADCUBA(nameFileMAdcube, true, false);
//				   //RMS SOLO SI YA NO SE HA PUESTO	        	
//			}
		 
			for(ResultSet resultdata: dbResultsArray)
				if(resultdata!=null)
					resultdata.close();
			IJ.showProgress(1);
	    } catch (SQLException sqlException) {
	    	Log.getInstance().logger.error(sqlException.getMessage());
	        
	    }	 finally {
	    	dbResultsArray.clear();
	    	if(newsqlcommand !=null )
	    		newsqlcommand.clear();
	    	params =null;
	    	newsqlcommand=null;
	    }
		return molecSpecies;
	}
	
	private static boolean doSearch(boolean isSameProduct,SearchSlimParams params, ArrayList<DataMolecular> dataMoleculesIni, ArrayRange freqRanges, 
					LineSearchPanel searchPanel, RowListStarTable criteriaSearch, boolean isMacro, boolean exists, boolean isParamsCheck,
					HashSet<String> moleculesSearch,HashSet<String> moleculesUpdate, HashSet<String> moleculesDelete) 
	{
		//IF IT IS NOT OPEN PANEL SEARCH; IT IS NECESSARY TO OPEN
		if(!searchPanel.getDialogResults().isVisible())
			searchPanel.getDialogResults().setVisible(true);	
		System.gc();
		String sqlcommand;
		Boolean isResult = false;

		if(moleculesSearch==null)
			return exists; //NO PUEDO RELLENARLO PORQUE ES NULO
		ArrayList<ResultSet> dbResultsArray = new ArrayList<ResultSet>();
		ArrayList<String> newsqlcommand =  null;
		ArrayList<DataMolecular> dataMolecules = new ArrayList<DataMolecular>();
		String catalogFin =  "";
		if(dataMoleculesIni!=null)
		{ //EXISTA OPCION JPL$* OR RECPMB$*  (IMPLEMENTARLO)
			for(int i=0; i<dataMoleculesIni.size(); i++)
			{
				  DataMolecular dataMol = new DataMolecular();
				  if(dataMoleculesIni.get(i).getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
						  continue;
				  catalogFin =dataMoleculesIni.get(i).getCatalog();
				  dataMol.setCatalog(catalogFin);
				  dataMol.setMolecula(dataMoleculesIni.get(i).getMolecula());
				  dataMol.setElow(dataMoleculesIni.get(i).getElow());
				  dataMol.setCm_1(dataMoleculesIni.get(i).getCm_1());
				  dataMol.setLog10(dataMoleculesIni.get(i).getLog10());
				  dataMol.setRenameMolecule(dataMoleculesIni.get(i).getRenameMolecule());

      			  if(dataMoleculesIni.get(i).getWHERE()!=null &&!dataMoleculesIni.get(i).getWHERE().isEmpty())
      				  dataMol.setWHERE("("+dataMoleculesIni.get(i).getWHERE()+")");
      			  else
      				  dataMol.setWHERE(dataMoleculesIni.get(i).getWHERE());
      				  
				  dataMolecules.add(dataMol);
				  //TODO TERMINAR DE RELLENAR
			}
			if(dataMolecules.isEmpty())
			{
				return exists;
			}				
		}
		try
		{
			//TODO THIS SHOULD NOT BE NEEDED. DB SHOULD BE MODIFIED SO NO DIFFERENT NAMES ARE NEEDED
			/*
			 * First gets the name of the table in DB corresponding to the Long Name selected in the Catalog Combo Box
			 */			
			sqlcommand = "SELECT id_tablename FROM TableIndex "+
							"WHERE id_tablelongname = '".concat(catalogFin.toUpperCase()).concat("'");

			ResultSet dbresult = searchPanel.getDB().query(sqlcommand);
			String row = catalogFin;//params.getCatalog();
//			Log.getInstance().logger.debug(exists+"="+sqlcommand);
			if(dbresult==null)
				return  exists;
			if(dbresult!=null&&dbresult.next())
            	row = dbresult.getObject(1).toString();
			//AQUI ES DODNE TENDRIAMOS QUE MRAR SI LA MOLECULA SIGUE EXISTIENDO EN EL CATALOGO, TANTO PARA TRANSFER COMO PARA ADD DATA
			//COMO LO PONGO AQUI, HAGO UN ACONSULTA A PARTE
			
//			sqlcommand = "SELECT id_tablename FROM TableIndex "+
//					"WHERE id_tablelongname = '".concat(catalogFin.toUpperCase()).concat("'");
//		
//			ResultSet dbresult = searchPanel.getDB().query(sqlcommand);
//			String row = catalogFin;//params.getCatalog();
//			if(dbresult.next())
//		    	row = dbresult.getObject(1).toString();
			
			
			
			//AQUI DENTRO CONSIDERO LOS WHERE
            newsqlcommand = generateSQLcommand(row, params, dataMolecules);
            
            //TODO THIS IS USING NEWSQLCOMMAND!!! SO MOST OF THE LINES UP THERE ARE DEPRECATED            
            int iCount = 1;
            int iFinal = newsqlcommand.size();
            for (String sql : newsqlcommand)
            {
        		IJ.showProgress(iCount/iFinal);
        		IJ.showStatus("MAKING QUERY/S:"+ iCount+" / "+iFinal);	

            	dbResultsArray = getFrequenciesAndQuery(sql,dbResultsArray,searchPanel.getDB(),freqRanges);
            	iCount++;
            }
            
            //TODO DEBE DECIR SI NUEVA O EXISTENTE
            
            //ANTES DE BUSCAR TIENE QUE INDICAR SI EL PRODUCTO NOS VALE O NO
            String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};
			//VER COMO SACAR EL PIXEL SI AQUI OR.....
    	
			isResult = searchPanel.getDialogResults().readSearchResult(isSameProduct,criteriaSearch,
             			dbResultsArray,
             			params.getSearchType(), nameFileMAdcube,isMacro, exists, true,
             			moleculesSearch, moleculesUpdate,  moleculesDelete);
			//DENTRO DE READ?? SOLO SI HA REALIZADO LA BUSQUEDA
			//if(isResult || exists)
			if(isResult & !exists && !isSameProduct)
			{
				   //CUIDADO AL BUSCAR, CUANDO SE ADD LOS PARAMS DEL RESULTADO
				   //NO DEBE CAMBIAR FILTER NI NOISE DEL RESTO, PERO SI DEL QUE HE ADD
				   //EN LA BUSQUEDA	
				   searchPanel.setNameFileMADCUBA(nameFileMAdcube, true, false);
				   //RMS SOLO SI YA NO SE HA PUESTO	        	
			}
		 
            dbresult.close();
            dbresult=null;
			for(ResultSet resultdata: dbResultsArray)
				if(resultdata!=null)
					resultdata.close();
			IJ.showProgress(1);
	    } catch (SQLException sqlException) {
	    	Log.getInstance().logger.error(sqlException.getMessage());
	        
	    }	 finally {
	    	dbResultsArray.clear();
	    	if(newsqlcommand !=null )
	    		newsqlcommand.clear();
	    	params =null;
	    	newsqlcommand=null;
	    }
		return isResult || exists;
    }
	private static MolecularSpecies doSearchMolecularSpecies(boolean isSameProduct,SearchSlimParams params, ArrayList<DataMolecular> dataMoleculesIni, ArrayRange freqRanges, 
			LineSearchPanel searchPanel, boolean isMacro, boolean exists, boolean isParamsCheck,
			HashSet<String> moleculesSearch) 
	{
		//IF IT IS NOT OPEN PANEL SEARCH; IT IS NECESSARY TO OPEN
		if(!searchPanel.getDialogResults().isVisible())
			searchPanel.getDialogResults().setVisible(true);	
		System.gc();
		String sqlcommand;
		Boolean isResult = false;
		
		if(moleculesSearch==null)
			return null; //NO PUEDO RELLENARLO PORQUE ES NULO
		ArrayList<ResultSet> dbResultsArray = new ArrayList<ResultSet>();
		ArrayList<String> newsqlcommand =  null;
		ArrayList<DataMolecular> dataMolecules = new ArrayList<DataMolecular>();
		String catalogFin =  "";
		MolecularSpecies molecularSpecies = null;
		if(dataMoleculesIni!=null)
		{ //EXISTA OPCION JPL$* OR RECPMB$*  (IMPLEMENTARLO)
			for(int i=0; i<dataMoleculesIni.size(); i++)
			{
				  DataMolecular dataMol = new DataMolecular();
				  if(dataMoleculesIni.get(i).getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
						  continue;
				  catalogFin =dataMoleculesIni.get(i).getCatalog();
				  dataMol.setCatalog(catalogFin);
				  dataMol.setMolecula(dataMoleculesIni.get(i).getMolecula());
				  dataMol.setElow(dataMoleculesIni.get(i).getElow());
				  dataMol.setCm_1(dataMoleculesIni.get(i).getCm_1());
				  dataMol.setLog10(dataMoleculesIni.get(i).getLog10());
				  dataMol.setRenameMolecule(dataMoleculesIni.get(i).getRenameMolecule());
				  dataMol.setWHERE(dataMoleculesIni.get(i).getWHERE());
				  dataMolecules.add(dataMol);
				  //TODO TERMINAR DE RELLENAR
			}
			if(dataMolecules.isEmpty())
			{
				return null;
			}				
		}
		try
		{
			//TODO THIS SHOULD NOT BE NEEDED. DB SHOULD BE MODIFIED SO NO DIFFERENT NAMES ARE NEEDED
			/*
			 * First gets the name of the table in DB corresponding to the Long Name selected in the Catalog Combo Box
			 */			
			sqlcommand = "SELECT id_tablename FROM TableIndex "+
							"WHERE id_tablelongname = '".concat(catalogFin.toUpperCase()).concat("'");
		
			ResultSet dbresult = searchPanel.getDB().query(sqlcommand);
			String row = catalogFin;//params.getCatalog();
		//	Log.getInstance().logger.debug(exists+"="+sqlcommand);
			if(dbresult==null)
				return  null;
			if(dbresult!=null&&dbresult.next())
		    	row = dbresult.getObject(1).toString();
			//AQUI ES DODNE TENDRIAMOS QUE MRAR SI LA MOLECULA SIGUE EXISTIENDO EN EL CATALOGO, TANTO PARA TRANSFER COMO PARA ADD DATA
			//COMO LO PONGO AQUI, HAGO UN ACONSULTA A PARTE
			
		//	sqlcommand = "SELECT id_tablename FROM TableIndex "+
		//			"WHERE id_tablelongname = '".concat(catalogFin.toUpperCase()).concat("'");
		//
		//	ResultSet dbresult = searchPanel.getDB().query(sqlcommand);
		//	String row = catalogFin;//params.getCatalog();
		//	if(dbresult.next())
		//    	row = dbresult.getObject(1).toString();
			
			
			
			//AQUI DENTRO CONSIDERO LOS WHERE
		    newsqlcommand = generateSQLcommand(row, params, dataMolecules);
		    
		    //TODO THIS IS USING NEWSQLCOMMAND!!! SO MOST OF THE LINES UP THERE ARE DEPRECATED            
		    int iCount = 1;
		    int iFinal = newsqlcommand.size();
		    for (String sql : newsqlcommand)
		    {
				IJ.showProgress(iCount/iFinal);
				IJ.showStatus("MAKING QUERY/S:"+ iCount+" / "+iFinal);	

			//	Log.getInstance().logger.debug("sql="+sql);
				
		    	dbResultsArray = getFrequenciesAndQuery(sql,dbResultsArray,searchPanel.getDB(),freqRanges);
		    	iCount++;
		    }
		    
		    //TODO DEBE DECIR SI NUEVA O EXISTENTE
		    
		    //ANTES DE BUSCAR TIENE QUE INDICAR SI EL PRODUCTO NOS VALE O NO
		    String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};
			//VER COMO SACAR EL PIXEL SI AQUI OR.....
		    molecularSpecies = searchPanel.getDialogResults().readSearchResult(
		     			dbResultsArray, nameFileMAdcube,isMacro, exists, true);
			
		 
		    dbresult.close();
		    dbresult=null;
			for(ResultSet resultdata: dbResultsArray)
				if(resultdata!=null)
					resultdata.close();
			IJ.showProgress(1);
		} catch (SQLException sqlException) {
			Log.getInstance().logger.error(sqlException.getMessage());
		    
		}	 finally {
			dbResultsArray.clear();
			if(newsqlcommand !=null )
				newsqlcommand.clear();
			params =null;
			newsqlcommand=null;
		}
		return molecularSpecies;
	}
	/**
     * Sends multiple queries in frequencies to the database
     * @param sqlcommand  --  String  Seach SQL command
	 * @param dbResultsArray --  is same return
	 * @param dbHSQLDBCreate -- data base
	 * @param freqRanges -- ArrayRange
     * @return  ArrayList Array of results from the DB SQL command.
     */
    public static ArrayList<ResultSet> getFrequenciesAndQuery(String sqlcommand,ArrayList<ResultSet> dbResultsArray, 
    			DbHSQLDBCreate dbHSQLDBCreate, ArrayRange freqRanges)
    {
        /*
         * Calls to the getFrequencyRange function and uses the returned ArrayRange
         * to perform the different searchs.
         */  	  	
        String sqlcommandFreq;
        try
        {
      	  /*
      	   * Order the frequencies and search
      	   * 
      	   */
        	if(freqRanges!=null)
      	   {
        	  for(Range _range: freqRanges.getArrayCollapsed())
	    	  {
        		  if(sqlcommand.contains("'NoNe'"))
        			  dbResultsArray.add(null);
        		  else
        		  {
        			  sqlcommandFreq = sqlcommand.concat(" id_frequency BETWEEN ").concat(_range.getStart().toString()).concat(" AND ").concat(_range.getEnd().toString());
        				
        			  sqlcommandFreq = sqlcommandFreq.concat(" ORDER BY id_formula");
//               		  Log.getInstance().logger.debug("B="+sqlcommandFreq);
               		  dbResultsArray.add(dbHSQLDBCreate.query(sqlcommandFreq));  
//               		  EditorInformationMADCUBA.append("SQL SEARCH:\n"+sqlcommandFreq.replace('\'', '$'));
//               		  Log.getInstance().logger.debug("A="+sqlcommandFreq);
        		  }
	    	  }
      	   } else {
//       		  EditorInformationMADCUBA.append("SQL SEARCH:\n"+sqlcommand);
      		   
      		 dbResultsArray.add(dbHSQLDBCreate.query(sqlcommand));
      	   }
        } catch (SQLException sqlException) {
      	  // TODO raise popup window with a "do not show this message again" checkbox
            // without that checkbox/option, the popup can be bother too much the user
//	        	popupErrorMessage("Problem while querying lines database: "
//                    + sqlException.getMessage());
        	sqlException.printStackTrace();
	    }
		return dbResultsArray;
    }
	/**
     * GENERATES THE SQL COMMAND(S) TO PERFORM
	 * @param tablename is String 
	 * @param params is SearchSlimParams
	 * @param dataMolecules is Array
	 * @return ArrayList is a list of String
     */
    private static ArrayList<String> generateSQLcommand(String tablename, SearchSlimParams params, 
    			ArrayList<DataMolecular> dataMolecules)
    {
  	  //TODO CUANDO VAYA A ANADIR LAS MOLECULAS SI DE DIGO ADD; NO BUSQUE LAS QUE EXISTAN
  	  	ArrayList<String> sqlcommandList = new ArrayList<String>();
  	
  	  	String sqlcommand = "SELECT * FROM " + tablename + " WHERE ";
  	  /*
  	   * ONLY FOR JPL AND CDMS OR USER (which is CDMS format)
  	   */
  	  	ModelLTESyntheticMoleculeFit panelModel = null;
		SLIMFRAME slimFrame = ((SLIMFRAME) WindowManager.getFrame(SlimConstants.FRAME_SLIM));
		if(slimFrame!=null)
		{
			if(slimFrame.getPanelModelLTESimFit()==null)
				slimFrame.charguePanelModelLTESimFit();
			panelModel = slimFrame.getPanelModelLTESimFit();
		} 
		int iColumnForm = -1;
		if(panelModel!=null)
			iColumnForm = SimFitParametersTable.columnFormula;

//		20220113 poniendo mire en lista
//		if(tablename.equals(SlimConstants.CATALOG_JPL) 
//        		|| tablename.equals(SlimConstants.CATALOG_CDMS) ||
//        		tablename.equals(SlimConstants.CATALOG_USER))//TODO ESTO ES LO DE SERGIO, TENGO QUE BUSCARLO EN LA LLAMADA
//        {
		int iPosTableName = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,tablename.toUpperCase());
		if(iPosTableName>=0)
		{//ESTA ES PAR PRINCIPAL, PERO PUEDEN VENIR DIFERENTES MOLECULAS SEGUN CATALOG
         	/*
        	 * To order the results of the search
        	 * as JPL INNER JOIN JPLcat is different
        	 * from JPLcat INNER JOIN JPL 
        	 */

//			if(tablename.toUpperCase().equals(SlimConstants.CATALOG_USER))
//			sqlcommand = sqlcommand.replace("*",
//	      			"id_TAG,id_frequency,id_ERR,id_LGINT,id_DR,id_ELO,id_GUP," +
//	      	      			"id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6," +
//	      	      			"id_TAG,id_formula, id_QNFMT,id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9,'"+tablename.toLowerCase()+"' as cata,"+
//	      	      			"'none' as id_rename"); //ESTO ESTA PUESTO PARA PODER TRAER EL NOMBRE QUE LE VOY A DAR
//	      	      
//			else
			if(existsQn7InCatalogs[iPosTableName])
				sqlcommand = sqlcommand.replace("*",
      			"id_TAG,id_frequency,id_ERR,id_LGINT,id_DR,id_ELO,id_GUP," +
      			"id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,id_QN7,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6,id_QNN7," +
      			"id_TAG,id_formula, id_QNFMT,id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9,'"+tablename.toLowerCase()+"' as cata,"+
      			"'none' as id_rename"); 
			else
				sqlcommand = sqlcommand.replace("*",
		      			"id_TAG,id_frequency,id_ERR,id_LGINT,id_DR,id_ELO,id_GUP," +
		      			"id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,' ' as id_QN7,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6,' ' as id_QNN7," +
		      			"id_TAG,id_formula, id_QNFMT,id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9,'"+tablename.toLowerCase()+"' as cata,"+
		      			"'none' as id_rename"); 
//			Log.getInstance().logger.debug("INITIAL sqlcommand="+sqlcommand);
			//ESTO ESTA PUESTO PARA PODER TRAER EL NOMBRE QUE LE VOY A DAR
        	//Y LO ENCUENTRE CUANDO ESTE GENERANDO LA TABLA
        	/*
        	 * Depending on whether we search ALL or individual, it is more efficient the JOIN
        	 * in different direction.
        	 */
      
        	  // To INNER JOIN the tables and compare the id_TAG from each table
        	for(String catalogT: SlimConstants.CATALOG_LIST_NO_RECOMB)
        	{
        		sqlcommand=replaceStringCatalog(sqlcommand,catalogT, catalogT+"cat INNER JOIN "+catalogT+" ON "+catalogT+".id_TAG="+catalogT+"cat.id_TAG");
        		//VER SI AQUI BASTARIA O HABRIA QUE CAMBIARLA EN OTRO SITIO
        	}
  	/*        	 * If Table is filled, the table search is performed.        	 */
          if (dataMolecules.size()>1)
          {
        	  String sForm = null;
        	  
              if(params.getSearchType().toLowerCase().equals("new") 
            		  || params.getSearchType().toLowerCase().contains("update"))
              {
            	  for (int irow=0 ; irow<dataMolecules.size(); irow++)
            	  {
            			sForm = dataMolecules.get(irow).getMolecula();
        				String command = generateSQLSearchCriteria(dataMolecules,sqlcommand, irow);
        				slimFrame.getPanelLineSearch();
						//Ver si existe con el catalog
        				ArrayList<String> listCatalog= LineSearchPanel.catalogsMolecule(sForm);
        				String catalog =dataMolecules.get(irow).getCatalog().toUpperCase();
        			//	Log.getInstance().logger.debug("catalog="+catalog+"=");
        				if(listCatalog.size()>0)
        				{
        					String commadAdd = command.concat("id_formula='"+ sForm +"' AND");
        					if(listCatalog.contains(catalog))
        					{
        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
        						{
        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
        							if(!catalog.equals(catalogTemp))
        							{
//        								commadAdd = commadAdd.replace(catalogTemp, catalog);
//        								commadAdd = commadAdd.replace(catalogTemp.toLowerCase(), catalog.toLowerCase());
        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp, catalog);
        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp.toLowerCase(), catalog.toLowerCase());
        								//IRIA AQUI EL CAMBIO DE COLUMNA 

        							}
        						}
        						int i = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog.toUpperCase());
        						if(i>=0)
        						{
									if(existsQn7InCatalogs[i] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
									{
										commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QN7,", ",id_QN7,");
										commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QNN7,", ",id_QNN7,");
	    								
									}else if(!existsQn7InCatalogs[i])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
									{
										commadAdd = replaceStringCatalog(commadAdd, ",id_QN7,",",' ' as id_QN7,");
										commadAdd = replaceStringCatalog(commadAdd, ",id_QNN7,",",' ' as id_QNN7,");
	    								
									}
        						}
        						sqlcommandList.add(commadAdd);
        					} else {
        						catalog = listCatalog.get(0);
        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
        						{
        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];       							
//        								commadAdd = commadAdd.replace(catalogTemp, catalog);
//        								commadAdd = commadAdd.replace(catalogTemp.toLowerCase(), catalog.toLowerCase());
       								commadAdd = replaceStringCatalog(commadAdd,catalogTemp, catalog);
       								commadAdd = replaceStringCatalog(commadAdd,catalogTemp.toLowerCase(), catalog.toLowerCase());
        						}
        						int i = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog.toUpperCase());
        						if(i>=0)
        						{
        							if(existsQn7InCatalogs[i] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
									{
										commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QN7,", ",id_QN7,");
										commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QNN7,", ",id_QNN7,");
	    								
									}else if(!existsQn7InCatalogs[i])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
									{
										commadAdd = replaceStringCatalog(commadAdd, ",id_QN7,",",' ' as id_QN7,");
										commadAdd = replaceStringCatalog(commadAdd, ",id_QNN7,",",' ' as id_QNN7,");
	    								
									}
        						}
        						sqlcommandList.add(commadAdd);
        					}
        				} else {

        					String commadAdd = command.concat("id_formula='"+ sForm +"' AND");

	          				// This is to get "FROM CATALOGcat INNER JOIN CATALOG ON CATALOG.id_TAG=CATALOGcat.id_TAG" in case it is not catalog
        					int iPosCatalog = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog.toUpperCase());
    						
        					if(iPosCatalog>=0)
        					{
        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
        						{
        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
        							if(!catalog.equals(catalogTemp))
        							{
//        								commadAdd = commadAdd.replace(catalogTemp, catalog);
//        								commadAdd = commadAdd.replace(catalogTemp.toLowerCase(), catalog.toLowerCase());
        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp, catalog);
        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp.toLowerCase(), catalog.toLowerCase());
        							}
        						}

        						if(existsQn7InCatalogs[iPosCatalog] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
								{
									commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QN7,", ",id_QN7,");
									commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QNN7,", ",id_QNN7,");
    								
								}else if(!existsQn7InCatalogs[iPosCatalog])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
								{
									commadAdd = replaceStringCatalog(commadAdd, ",id_QN7,",",' ' as id_QN7,");
									commadAdd = replaceStringCatalog(commadAdd, ",id_QNN7,",",' ' as id_QNN7,");
    								
								}
        						sqlcommandList.add(commadAdd);
        					}

        				}
            	  }
              } else {
            	  
               	  ArrayList<Integer> resultSearch = null;
               	  String sRenameMolecule = "";
            	  for (int irow=0 ; irow<dataMolecules.size(); irow++)
            	  {
            		  sForm =  dataMolecules.get(irow).getMolecula();
            		  //No tengo que buscar sFrom si su renombre
            		  sRenameMolecule  = dataMolecules.get(irow).getRenameMolecule();
            		  if(panelModel!=null)
            		  {
            			  if(sRenameMolecule==null || sRenameMolecule.toLowerCase().equals("none")
            					  || sRenameMolecule.isEmpty() || sRenameMolecule.toLowerCase().equals("any"))
            			  {
            				  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);
            				  if(resultSearch==null || resultSearch.size()<1)
            					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, "Rem_"+sForm);
            			  }
            			  else
            			  {
            				  if(sRenameMolecule.toLowerCase().startsWith("rem_"))
            				  {
            					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);

                            	  if(resultSearch==null || resultSearch.size()<1)
                            		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);

            				  } else {
            					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
            				  }
            			  }
            		  }
                	  if(resultSearch==null || resultSearch.size()<1)
            		  {
	          				String command = generateSQLSearchCriteria(dataMolecules, sqlcommand, irow);
	          				ArrayList<String> listCatalog= LineSearchPanel.catalogsMolecule(sForm);
	        				String catalog =dataMolecules.get(irow).getCatalog().toUpperCase();
	        				if(listCatalog.size()>0)
	        				{
	        					String commadAdd = command.concat("id_formula='"+ sForm +"' AND");
	        					if(listCatalog.contains(catalog))
	        					{
	        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
	        						{
	        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
	        							if(!catalog.equals(catalogTemp))
	        							{
	        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp, catalog);
	        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp.toLowerCase(), catalog.toLowerCase());
	        							}
	        						}
	        						int i = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog.toUpperCase());
	        						if(i>=0)
	        						{
	        							if(existsQn7InCatalogs[i] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
										{
											commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QN7,", ",id_QN7,");
											commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QNN7,", ",id_QNN7,");
		    								
										}else if(!existsQn7InCatalogs[i])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
										{
											commadAdd = replaceStringCatalog(commadAdd, ",id_QN7,",",' ' as id_QN7,");
											commadAdd = replaceStringCatalog(commadAdd, ",id_QNN7,",",' ' as id_QNN7,");
		    								
										}
	        						}
	        						sqlcommandList.add(commadAdd);
	        					} else {
	        						catalog = listCatalog.get(0);
	        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
	        						{
	        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];        							
//	        								commadAdd = commadAdd.replace(catalogTemp, catalog);
//	        								commadAdd = commadAdd.replace(catalogTemp.toLowerCase(), catalog.toLowerCase());
	        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp, catalog);
	        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp.toLowerCase(), catalog.toLowerCase());
	        						}
	        						int i = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog.toUpperCase());
	        						if(i>=0)
	        						{
	        							if(existsQn7InCatalogs[i] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
										{
											commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QN7,", ",id_QN7,");
											commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QNN7,", ",id_QNN7,");
		    								
										}else if(!existsQn7InCatalogs[i])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
										{
											commadAdd = replaceStringCatalog(commadAdd, ",id_QN7,",",' ' as id_QN7,");
											commadAdd = replaceStringCatalog(commadAdd, ",id_QNN7,",",' ' as id_QNN7,");
		    								
										}
	        						}
	        						sqlcommandList.add(commadAdd);
	        					}
	        					
	        				} else {
	        					String commadAdd = command.concat("id_formula='"+ sForm +"' AND");

		          				// This is to get "FROM CATALOGcat INNER JOIN CATALOG ON CATALOG.id_TAG=CATALOGcat.id_TAG" in case it is not catalog
	        					int iPosCatalog = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog.toUpperCase());
        						
	        					if(iPosCatalog>=0)
	        					{
	        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
	        						{
	        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
	        							if(!catalog.equals(catalogTemp))
	        							{
//	        								commadAdd = commadAdd.replace(catalogTemp, catalog);
//	        								commadAdd = commadAdd.replace(catalogTemp.toLowerCase(), catalog.toLowerCase());
	        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp, catalog);
	        								commadAdd = replaceStringCatalog(commadAdd,catalogTemp.toLowerCase(), catalog.toLowerCase());
	        							}
	        						}
	        						if(existsQn7InCatalogs[iPosCatalog] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
									{
										commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QN7,", ",id_QN7,");
										commadAdd = replaceStringCatalog(commadAdd,",' ' as id_QNN7,", ",id_QNN7,");
	    								
									}else if(!existsQn7InCatalogs[iPosCatalog])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
									{
										commadAdd = replaceStringCatalog(commadAdd, ",id_QN7,",",' ' as id_QN7,");
										commadAdd = replaceStringCatalog(commadAdd, ",id_QNN7,",",' ' as id_QNN7,");
	    								
									}
	        						sqlcommandList.add(commadAdd);
	        					}
	        				}	
            		  } else  {
	          				String command = generateSQLSearchCriteria(dataMolecules, sqlcommand, irow);
  	          					sqlcommandList.add(
  	          						command.concat("id_formula='NoNe' AND")
  	          						);
            		  }
            	  }
              }          				
          } else  {
				 if(dataMolecules.size()>0)
				 {
		        	sqlcommand = generateSQLSearchCriteria(dataMolecules,sqlcommand, 0);
					if (!dataMolecules.get(0).getMolecula().contains("*"))
                	{
						if(params.getSearchType().toLowerCase().equals("new") || params.getSearchType().toLowerCase().contains("update"))
				        {
            				sqlcommand = sqlcommand.concat("id_formula='").concat(dataMolecules.get(0).getMolecula()).concat("' AND ");
            			} else {
            				String sForm =  dataMolecules.get(0).getMolecula();
            				String sRenameMolecule =  dataMolecules.get(0).getRenameMolecule();
                       	
            				ArrayList<Integer> resultSearch = null;

                  		  	if(panelModel!=null)
                  		  	{
	                  		  	if(sRenameMolecule==null || sRenameMolecule.toLowerCase().equals("none")
	                  		  		 || sRenameMolecule.isEmpty() || sRenameMolecule.toLowerCase().equals("any"))
	                  		  	{
	                  		  		resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);

	                  		  		if(resultSearch==null || resultSearch.size()<1)
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, "Rem_"+sForm);
	                  		  	}
	                  		  	else
	                  		  	{
	              				  if(sRenameMolecule.toLowerCase().startsWith("rem_"))
	              				  {
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);

	                              	  if(resultSearch==null || resultSearch.size()<1)
	                              		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);

	              				  } else {
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
	              				  }
	              			  }
                  		  	}                  		  	
            				if(resultSearch==null || resultSearch.size()<1)
	              			{
	              				sqlcommand = sqlcommand.concat("id_formula='").concat(dataMolecules.get(0).getMolecula()).concat("' AND ");	                  			
	              			} else {
	              				sqlcommand = sqlcommand.concat("id_formula='").concat("NoNe").concat("' AND ");
	              			}
            			}
						ArrayList<String> listCatalog= LineSearchPanel.catalogsMolecule(dataMolecules.get(0).getMolecula());
        				String catalog =dataMolecules.get(0).getCatalog().toUpperCase();        				
        				if(listCatalog.size()>0)
        				{
        					if(listCatalog.contains(catalog))
        					{
        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
        						{
        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
        							if(!catalog.equals(catalogTemp))
        							{
        								sqlcommand = replaceStringCatalog(sqlcommand,catalogTemp, catalog);
                			        	sqlcommand = replaceStringCatalog(sqlcommand,catalogTemp.toLowerCase(), catalog.toLowerCase());
        							}
        						}
        					} else {
        						catalog = listCatalog.get(0);
        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
        						{
        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i]; 
        							sqlcommand = replaceStringCatalog(sqlcommand,catalogTemp, catalog);
            			        	sqlcommand = replaceStringCatalog(sqlcommand,catalogTemp.toLowerCase(), catalog.toLowerCase());
        						}
        					}

	        			} else {
	        				
	          				// This is to get "FROM CATALOGcat INNER JOIN CATALOG ON CATALOG.id_TAG=CATALOGcat.id_TAG" in case it is not catalog
        					if(Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog)>=0)
        						
        					{
        						for(int i=SlimConstants.CATALOG_LIST_NO_RECOMB.length-1; i>=0; i--)        							
        						{
        							String catalogTemp = SlimConstants.CATALOG_LIST_NO_RECOMB[i];
        							if(!catalog.equals(catalogTemp))
        							{ 
        								sqlcommand = replaceStringCatalog(sqlcommand,catalogTemp, catalog);
                			        	sqlcommand = replaceStringCatalog(sqlcommand,catalogTemp.toLowerCase(), catalog.toLowerCase());
        							}
        						}
        					}
        				}
        				int i = Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,catalog);
						if(i>=0)
						{
							if(existsQn7InCatalogs[i] )//&& commadAdd.toLowerCase().indexOf("id_qn7")<0)
							{
								sqlcommand = replaceStringCatalog(sqlcommand,",' ' as id_QN7,", ",id_QN7,");
								sqlcommand = replaceStringCatalog(sqlcommand,",' ' as id_QNN7,", ",id_QNN7,");
								
							}else if(!existsQn7InCatalogs[i])// && commadAdd.toLowerCase().indexOf("id_qn7")>0)
							{
								sqlcommand = replaceStringCatalog(sqlcommand, ",id_QN7,",",' ' as id_QN7,");
								sqlcommand = replaceStringCatalog(sqlcommand, ",id_QNN7,",",' ' as id_QNN7,");
								
							}
						}
                 	}
        			/*
        			 * Otherwise we search for something containing whatever is written in the combo 
        			 */
        			else
        			{
        				sqlcommand = sqlcommand.concat("id_formula LIKE '%").concat(dataMolecules.get(0).getMolecula().replace("*", "")).concat("%' AND ");
        				/*
        				 * We better change the JOIN order to make it similar to the ALL search
        				 */
        		       	for(String catalogT: SlimConstants.CATALOG_LIST_NO_RECOMB)
            				sqlcommand = sqlcommand.replace(catalogT+"cat INNER JOIN "+catalogT+" ON "+catalogT+".id_TAG="+catalogT+"cat.id_TAG",catalogT+" INNER JOIN "+catalogT+"cat ON "+catalogT+".id_TAG="+catalogT+"cat.id_TAG");
        		       	//AUI ME QUEDA VER CUANDO ENTRA PARA VER SI HAY QUE CAMBIARLO
//        		       	Log.getInstance().logger.debug("ENTRA AQUI CON % MIRAR");
        		       	//ME FALTA EXCLUIR AQUELLAS QUE YA ESTEN EN LA TABLA 
        				if(!params.getSearchType().toLowerCase().equals("new") && !params.getSearchType().toLowerCase().contains("update"))
				        {
            				String sForm =  dataMolecules.get(0).getMolecula().replace("*", "");
                       	
            				ArrayList<Integer> resultSearch = null;

                  		  	if(panelModel!=null)
                  		  	{
                  		  		resultSearch = panelModel.sFitter.simJTParam.searchRow(iColumnForm, sForm);

                  		  		if(resultSearch==null || resultSearch.size()<1)
              					  resultSearch = panelModel.sFitter.simJTParam.searchRow(iColumnForm, "Rem_"+sForm);
                  		  		if(resultSearch.size()>0)
                  		  		{
                  		  			sqlcommand = sqlcommand.concat("NOT id_formula IN (");
                  		  			for(int row : resultSearch)
                  		  			{
                  		  				sqlcommand = sqlcommand.concat("'"+panelModel.sFitter.simJTParam.getValueAt(row, iColumnForm)+"',");
                  		  			}
                  		  			sqlcommand =sqlcommand.substring(0,sqlcommand.length()-1)+") AND ";
                  		  		}
                  		  	}  
				        }
        			}
            		sqlcommandList.add(sqlcommand);
        		} else {
        			sqlcommand = generateSQLSearchCriteria(params.getCriteriaDefault(),sqlcommand, 0);
            		//ADD WHERE Y RENAME
        			if(params.getWHERE()!=null)
        				sqlcommand = sqlcommand.concat(params.getWHERE()+" AND ");
        			if(params.getNewNameMolecule()!=null 
        					&& !params.getNewNameMolecule().isEmpty()
        					&& !params.getNewNameMolecule().toLowerCase().equals("none"))
        				sqlcommand = sqlcommand.replaceFirst("none",params.getNewNameMolecule());
        			sqlcommandList.add(sqlcommand);
        		}
        	}
        }
        /*
         * FOR OTHER CATALOGS
         */
        else
        {
        	//EN LOVAS VER SI TIENE O NO CATALOG QN7, ¿COMO HACERLO?
        	sqlcommand = "SELECT ID_FREQUENCY, ID_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, ID_ENERGY, ID_AIJ  FROM " + tablename + " WHERE ";
        	if(params.getSearchType().toLowerCase().equals("new") || params.getSearchType().toLowerCase().contains("update"))
            {
        		if(tablename.equals("Lovas1992")
      					|| tablename.equals("SmallLovas92")
      					|| tablename.equals("Lovas2003")
      					|| tablename.equals("SmallLovas03"))
        			sqlcommand = sqlcommand.replace("ID_ENERGY, ID_AIJ", "0.0  as lab_ELO, 0.0 as lab_AIJ");
            	  else
            		  sqlcommand = sqlcommand.replace("ID_FREQUENCYUNCERT", "0.0  as lab_FREQUENCYUNCERT");
        		
	        	  if (dataMolecules.size()>1)
	        	  {
	            		for (int irow=0 ; irow<dataMolecules.size(); irow++)
	            		{
	            			if(dataMolecules.get(irow).getCatalog().equals("Lovas1992")
	            					|| dataMolecules.get(irow).getCatalog().equals("SmallLovas92"))
	            			{
	            				sqlcommand = "SELECT ID_FREQUENCY, ID_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, 0.0  as lab_ELO, 0.0 as lab_AIJ";
	            				if(dataMolecules.get(0).getRenameMolecule()!=null 
		                				&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
		                				&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
	            					sqlcommand +=",'"+dataMolecules.get(0).getRenameMolecule()+"' as id_rename";
	            				else
	            					sqlcommand +=",'none' as id_rename";
	            				sqlcommand+= " FROM SmallLovas92 WHERE id_formula='"+ dataMolecules.get(irow).getMolecula() +"' AND";
	            				//ADD WHERE
	            	        	if(dataMolecules.get(0).getWHERE()!=null)
	            	        				sqlcommand = sqlcommand.concat(" "+dataMolecules.get(0).getWHERE()+" AND ");
	            				sqlcommandList.add(sqlcommand);
	            			}
	            			else if(dataMolecules.get(irow).getCatalog().equals("Lovas2003")
	            					|| dataMolecules.get(irow).getCatalog().equals("SmallLovas03"))
	            			{
	            				sqlcommand = "SELECT ID_FREQUENCY, ID_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, 0.0  as lab_ELO, 0.0 as lab_AIJ";
	            				if(dataMolecules.get(0).getRenameMolecule()!=null 
		                				&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
		                				&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
	            					sqlcommand +=",'"+dataMolecules.get(0).getRenameMolecule()+"' as id_rename";
	            				else
	            					sqlcommand +=",'none' as id_rename";
	            				sqlcommand+= " FROM SmallLovas03 WHERE id_formula='"+ dataMolecules.get(irow).getMolecula() +"' AND";
	            				//ADD WHERE
	            	        	if(dataMolecules.get(0).getWHERE()!=null)
	            	        				sqlcommand = sqlcommand.concat(" "+dataMolecules.get(0).getWHERE()+" AND ");
	            				sqlcommandList.add(sqlcommand);
	            			}
	            			else if(dataMolecules.get(irow).getCatalog().equals("BigLovas"))
	            			{
	            				sqlcommand = "SELECT ID_FREQUENCY,  0.0 as lab_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, ID_ENERGY, ID_AIJ";
	            				if(dataMolecules.get(0).getRenameMolecule()!=null 
		                				&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
		                				&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
	            					sqlcommand +=",'"+dataMolecules.get(0).getRenameMolecule()+"' as id_rename";
	            				else
	            					sqlcommand +=",'none' as id_rename";
	            				sqlcommand+= " FROM BigLovas WHERE id_formula='"+ dataMolecules.get(irow).getMolecula() +"' AND";
	            				//ADD WHERE
	            	        	if(dataMolecules.get(0).getWHERE()!=null)
	            	        				sqlcommand = sqlcommand.concat(" "+dataMolecules.get(0).getWHERE()+" AND ");
	            				sqlcommandList.add(sqlcommand);	            				
	            			}
	            		}
	              }
	              else if(dataMolecules.size()>0 
	        	            	&& !dataMolecules.get(0).getMolecula().equals(""))
	        	  {
		        		//ADD WHERE
	        			if(dataMolecules.get(0).getWHERE()!=null)
	        				sqlcommand = sqlcommand.concat(dataMolecules.get(0).getWHERE()+" AND ");
	        		  /*
	        		   * Same as last case above
	        		   * If one item is selected we search that item
	        		   */
	        		  if (!dataMolecules.get(0).getMolecula().contains("*"))
	        		  {
	                    	sqlcommand = sqlcommand.concat("id_formula='").concat(dataMolecules.get(0).getMolecula()).concat("' AND ");
	                    	//ADD  RENAME
	                    	if(dataMolecules.get(0).getRenameMolecule()!=null 
	                				&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
	                				&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
	                    		sqlcommand = sqlcommand.replaceFirst("none",dataMolecules.get(0).getRenameMolecule());
	        		  } else {
		        		  /*
		        		   * Otherwise we search for something containing whatever is written in the combo 
		        		   */
	        			  sqlcommand = sqlcommand.concat("id_formula LIKE '%").concat(dataMolecules.get(0).getMolecula().replace("*", "")).concat("%' AND ");
	        		  }
	        		  sqlcommandList.add(sqlcommand);
	        	  } else { 
	        		//ADD WHERE Y RENAME
	        			if(params.getWHERE()!=null)
	        				sqlcommand = sqlcommand.concat(params.getWHERE()+" AND ");
	        			if(params.getNewNameMolecule()!=null 
	        					&& !params.getNewNameMolecule().isEmpty()
	        					&& !params.getNewNameMolecule().toLowerCase().equals("none"))
	        				sqlcommand = sqlcommand.replaceFirst("none",params.getNewNameMolecule());
	      			  sqlcommandList.add(sqlcommand);
	        	  }
  		  }  else	  {
  			 if (dataMolecules.size()>1)
  	         {
  				  ArrayList<Integer> resultSearch = null;    	
	          	  String sForm = "NoNe";
	          	  String sRenameMolecule = "none";
	          	  for (int irow=0 ; irow<dataMolecules.size(); irow++)
	          	  {
	          		  sForm =  dataMolecules.get(irow).getMolecula();
	          		  sRenameMolecule =  dataMolecules.get(irow).getRenameMolecule();

	        		  if(panelModel!=null)
	        		  {
	        			  	if(sRenameMolecule==null || sRenameMolecule.toLowerCase().equals("none")
	        			  			 || sRenameMolecule.isEmpty() || sRenameMolecule.toLowerCase().equals("any"))
	        			  	{
	        			  		resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);

	              				if(resultSearch==null || resultSearch.size()<1)
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, "Rem_"+sForm);
	        			  	}  	else    			  {
	              				  if(sRenameMolecule.toLowerCase().startsWith("rem_"))
	              				  {
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);

	                              	  if(resultSearch==null || resultSearch.size()<1)
	                              		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);

	              				  } else {
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
	              				  }
	              			  }
	        		  }
	        		
	        		  sqlcommand = "SELECT ID_FREQUENCY, ID_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, 0.0  as lab_ELO, 0.0 as lab_AIJ";
	        			if(dataMolecules.get(0).getRenameMolecule()!=null 
                				&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
                				&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
        					sqlcommand +=",'"+dataMolecules.get(0).getRenameMolecule()+"' as id_rename";
        				else
        					sqlcommand +=",'none' as id_rename";
	        			sqlcommand +=" FROM ";
	                if(dataMolecules.get(irow).getCatalog().equals("Lovas1992")
          					|| dataMolecules.get(irow).getCatalog().equals("SmallLovas92"))
	                	  sqlcommand+= "SmallLovas92 WHERE ";
          			else if(dataMolecules.get(irow).getCatalog().equals("Lovas2003")
          					|| dataMolecules.get(irow).getCatalog().equals("SmallLovas03"))
	                	  sqlcommand+= "SmallLovas03 WHERE ";
          			else if(dataMolecules.get(irow).getCatalog().equals("BigLovas"))
          			{
	                	  sqlcommand = "SELECT ID_FREQUENCY,  0.0 as lab_FREQUENCYUNCERT, ID_FORMULA, ID_TRANSITION, ID_ENERGY, ID_AIJ";
	                	  if(dataMolecules.get(0).getRenameMolecule()!=null 
	                				&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
	                				&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
	        					sqlcommand +=",'"+dataMolecules.get(0).getRenameMolecule()+"' as id_rename";
	        				else
	        					sqlcommand +=",'none' as id_rename";
		        			sqlcommand +=" FROM BigLovas WHERE ";
          			} 
	                if(resultSearch==null || resultSearch.size()<1)
	          		{
		          		String command = generateSQLSearchCriteria(dataMolecules, sqlcommand, irow);
       					sqlcommandList.add( command.concat("id_formula='"+ sForm +"' AND") );
	          		  } else  {
	          			  String sMe = sForm;
	          			  if(sRenameMolecule!=null && !sRenameMolecule.isEmpty()
	          					  && !sRenameMolecule.toLowerCase().equals("none")
	          					  && !sRenameMolecule.toLowerCase().equals("any"))
	          				sMe = sRenameMolecule +"("+sForm+")";
	          				
            			  EditorInformationMADCUBA.append("Exist a Formule '"+sMe+"' in search");
	          			  String command = generateSQLSearchCriteria(dataMolecules, sqlcommand, irow);
	          			  sqlcommandList.add( command.concat("id_formula='NoNe' AND") );		          				
	          		  }
	          	  }
  	          } else {
	  			  String sForm = "NoNe";
	  			  if(tablename.equals("Lovas1992")
      					|| tablename.equals("SmallLovas92")
      					|| tablename.equals("Lovas2003")
      					|| tablename.equals("SmallLovas03"))
	  				sqlcommand =  sqlcommand.replace("ID_ENERGY, ID_AIJ", "0.0  as lab_ELO, 0.0 as lab_AIJ, 'none' id_rename");
            	  else
            		  sqlcommand =  sqlcommand.replace("ID_FREQUENCYUNCERT", "0.0  as lab_FREQUENCYUNCERT, 'none' id_rename");

        		  ArrayList<Integer> resultSearch=null;
	  			  if (dataMolecules.size()>0 && !dataMolecules.get(0).getMolecula().contains("*"))
	  			  { 
	  				  sForm = dataMolecules.get(0).getMolecula();
	  				  String sRenameMolecule =  dataMolecules.get(0).getRenameMolecule();
		          		
	        		  if(panelModel!=null){
	        			  	if(sRenameMolecule==null || sRenameMolecule.toLowerCase().equals("none")
	        			  			 || sRenameMolecule.isEmpty() || sRenameMolecule.toLowerCase().equals("any"))
	        			  	{
	        			  		resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);

	            				if(resultSearch==null || resultSearch.size()<1)
	            					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, "Rem_"+sForm);
	        			  	}else {
	              				  if(sRenameMolecule.toLowerCase().startsWith("rem_"))
	              				  {
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);

	                              	  if(resultSearch==null || resultSearch.size()<1)
	                              		  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sForm);
	              				  } else {
	              					  resultSearch = panelModel.sFitter.simJTParam.searchRowEquals(iColumnForm, sRenameMolecule);
	              				  }
	              			  }
	        		  }
	        		  
	  				  if(resultSearch==null || resultSearch.size()<1)
	  				  {
		                    	sqlcommand = sqlcommand.concat("id_formula='").concat(sForm).concat("' AND ");
	  				  } else {
	  					 String sMe = sForm;
	          			  if(sRenameMolecule!=null && !sRenameMolecule.isEmpty()
	          					  && !sRenameMolecule.toLowerCase().equals("none")
	          					  && !sRenameMolecule.toLowerCase().equals("any"))
	          				sMe = sRenameMolecule +"("+sForm+")";
            			  EditorInformationMADCUBA.append("Exist a Formule '"+sMe+"' in search");
	  					  sqlcommand = sqlcommand.concat("id_formula='").concat("NoNe").concat("' AND ");
	  				  }
	  				  if(dataMolecules.get(0).getWHERE()!=null)
	  					sqlcommand = sqlcommand.concat(dataMolecules.get(0).getWHERE()+" AND ");
	  				  if(dataMolecules.get(0).getRenameMolecule()!=null 
	  						&& !dataMolecules.get(0).getRenameMolecule().isEmpty()
	  						&& !dataMolecules.get(0).getRenameMolecule().toLowerCase().equals("none"))
	  					sqlcommand = sqlcommand.replaceFirst("none",dataMolecules.get(0).getRenameMolecule());
	  			  }
	  			  /*
	  			   *Otherwise we search for something containing whatever is written in the combo 
	  			   */
	  			  else if(dataMolecules.size()>0){
		        			sqlcommand = sqlcommand.concat("id_formula LIKE '%").concat(dataMolecules.get(0).getMolecula().replace("*", "")).concat("%' AND ");
		        			if(dataMolecules.get(0).getWHERE()!=null)
			  					sqlcommand = sqlcommand.concat(dataMolecules.get(0).getWHERE()+" AND ");
	  			  }
	  			  sqlcommandList.add(sqlcommand);
  	          }
  		  }
        }
        return sqlcommandList;
    }
    public static boolean validateExistsQn7(String tablename, DbHSQLDBCreate dbHSQLDBCreate) {

    	boolean exists = true;
  	  	String sqlcommand = "SELECT TOP 1 id_QN7, id_QNN7 FROM " + tablename +";";
  	  	ResultSet dbresult = null;
		try {
//			Log.getInstance().logger.debug(sqlcommand);
			dbresult = dbHSQLDBCreate.query(sqlcommand);
			if(dbresult ==null)
				exists = false;
		} catch (SQLException e) {
			exists = false;
		}finally {
			if(dbresult!=null)
			{
				try {
					dbresult.close();
				} catch (SQLException e) {
				}
			}
		}
		return exists;
	}

	private static String generateSQLSearchCriteria(ArrayList<DataMolecular> dataMolecules, String sqlcommand, int irow) 
	{
		/*
	     * IF THERE ARE ENERGY CONSTRAINTS
	     */
		String energy = "";
		if(dataMolecules.get(irow).getElow()!=null)
			energy = dataMolecules.get(irow).getElow();
		String energy2 = "";
		if(dataMolecules.get(irow).getCm_1()!=null)
			energy2 = dataMolecules.get(irow).getCm_1();
		
		if((energy !=null && !energy.toUpperCase().equals("ANY") && !energy.replace(" ", "").equals("")) ||
				(energy2 !=null && !energy2.toUpperCase().equals("ANY") && !energy2.replace(" ", "").equals("")))
	    {
	    	Float lowE, upE;
	    	try{
	    		if(energy == null || energy.toUpperCase().equals("ANY"))
	    			lowE = new Float(0);
	    		else
	    			lowE = Float.parseFloat(energy);
	    	}catch (NumberFormatException nfException) {
	    		lowE = new Float(0);
	    	}
	    	try{
	    		if(energy2 == null || energy2.toUpperCase().equals("ANY"))
	    			upE = new Float(100000);
	    		else
	    			upE = Float.parseFloat(energy2);
	    	}catch (NumberFormatException nfException) {
	    		upE = new Float(100000);
	    	}
	    	if (lowE.compareTo(upE)>0 )
	    	{
	    		float temp = lowE;
	    		lowE = upE;
	    		upE = temp;
	    	}
	    	sqlcommand = sqlcommand.concat(" id_ELO BETWEEN ").concat(lowE.toString()).concat(" AND ").concat(upE.toString()).concat(" AND ");
	    	
	    }
	    
	    /*
      	 * IF THERE ARE INTENSITY CONSTRAINTS
      	 */
	    String intensity = "";
		if(dataMolecules.get(irow).getLog10()!=null)
			intensity = dataMolecules.get(irow).getLog10();
 
        if(intensity!=null && !intensity.toUpperCase().equals("ANY") && !intensity.equals("") /*&& tfIntensityT.getText().equals("300")*/)
        {
        	try{
        		Float.parseFloat(intensity);
        		sqlcommand = sqlcommand.concat(" id_LGINT > ").concat(intensity).concat(" AND ");
        	}catch (NumberFormatException nfException) {
        		/*tfIntensity.setText("Any");*/
        		Log.getInstance().logger.error(nfException.getMessage());
        	}
        }
        
        
      //AHORA ADD EL WHERE Y EL RENAME DE LA FORMULA SI LO HUBIERA

    	   
		if(dataMolecules.get(irow).getWHERE()!=null && !dataMolecules.get(irow).getWHERE().isEmpty())
			sqlcommand = sqlcommand.concat(dataMolecules.get(irow).getWHERE()).concat(" AND ");
		
		if(dataMolecules.get(irow).getRenameMolecule()!=null 
				&& !dataMolecules.get(irow).getRenameMolecule().isEmpty()
				&& !dataMolecules.get(irow).getRenameMolecule().toLowerCase().equals("none"))
		{
			sqlcommand = sqlcommand.replaceFirst("none",dataMolecules.get(irow).getRenameMolecule());
		}
	    return sqlcommand;
    }
    
    private static String generateSQLSearchCriteria(String paramsCriteria, String sqlcommand, int irow) 
	{
    	
    	String[] slist =  MyUtilities.createArraysStringValues(paramsCriteria,MyConstants.SEPARATOR_NIVEL_2);
		  
		/*
	     * IF THERE ARE ENERGY CONSTRAINTS
	     */
		String energy = "";
		if(slist[0]!=null)
			energy = slist[0];
		String energy2 = "";
		if(slist[1]!=null)
			energy2 = slist[1];
		
		if((energy !=null && !energy.toUpperCase().equals("ANY") && !energy.replace(" ", "").equals("")) ||
				(energy2 !=null && !energy2.toUpperCase().equals("ANY") && !energy2.replace(" ", "").equals("")))
	    {
	    	Float lowE, upE;
	    	try{
	    		if(energy == null || energy.toUpperCase().equals("ANY"))
	    			lowE = new Float(0);
	    		else
	    			lowE = Float.parseFloat(energy);
	    	}catch (NumberFormatException nfException) {
	    		lowE = new Float(0);
	    	}
	    	try{
	    		if(energy2 == null || energy2.toUpperCase().equals("ANY"))
	    			upE = new Float(100000);
	    		else
	    			upE = Float.parseFloat(energy2);
	    	}catch (NumberFormatException nfException) {
	    		upE = new Float(100000);
	    	}
	    	if (lowE.compareTo(upE)>0 )
	    	{
	    		float temp = lowE;
	    		lowE = upE;
	    		upE = temp;
	    	}
	    	sqlcommand = sqlcommand.concat(" id_ELO BETWEEN ").concat(lowE.toString()).concat(" AND ").concat(upE.toString()).concat(" AND ");
	    	
	    }
	    
	    /*
      	 * IF THERE ARE INTENSITY CONSTRAINTS
      	 */
	    String intensity = "";
		if(slist[2]!=null)
			intensity = slist[2];
 
        if(intensity!=null && !intensity.toUpperCase().equals("ANY") && !intensity.equals("") /*&& tfIntensityT.getText().equals("300")*/)
        {
        	try{
        		Float.parseFloat(intensity);
        		sqlcommand = sqlcommand.concat(" id_LGINT > ").concat(intensity).concat(" AND ");
        	}catch (NumberFormatException nfException) {
        		/*tfIntensity.setText("Any");*/
        		Log.getInstance().logger.error(nfException.getMessage());
        	}
        }
	    return sqlcommand;
    }

	private static boolean doSearchRecombLines(boolean isSameProduct, SearchSlimParams params, ArrayList<DataMolecular> dataMoleculesIni, ArrayRange freqRanges, 
			LineSearchPanel searchPanel, RowListStarTable criteriaSearch, boolean isMacro, 
			HashSet<String> moleculesSearch, HashSet<String> moleculesUpdate, HashSet<String> moleculesDelete,boolean isParamsCheck) 
	{
		  if(moleculesSearch ==null)
			  return false; //NO TENGO MOLECULEAS Y NO PUEDO CREARLO AQUI DENTRO TIENE QUE SER ANTES DE ENTRAR CON LO CUAL PUEDE DAR PROBELAMS
		  if(!searchPanel.getDialogResults().isVisible())
			  searchPanel.getDialogResults().setVisible(true);
		  System.gc();
		  //EXTRACT DATA PARAMS
		  ArrayList<DataMolecular> dataMolecules = new ArrayList<DataMolecular>();
		  int deltaN = -1;

		  if(dataMoleculesIni!=null)
		  {
			  for(int i=0; i<dataMoleculesIni.size(); i++)
			  {
				  DataMolecular dataMol = new DataMolecular();
//				  Log.getInstance().logger.debug("dataMoleculesIni.get(i).getCatalog()="+dataMoleculesIni.get(i).getCatalog());
//				  Log.getInstance().logger.debug("dataMoleculesIni.get(i).getMolecula()="+dataMoleculesIni.get(i).getMolecula());
					
				  if(!dataMoleculesIni.get(i).getCatalog().toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase()))
					  continue;
				  dataMol.setCatalog(dataMoleculesIni.get(i).getCatalog());
//				  if(dataMoleculesIni.get(i).getMolecula().length()>2)
//				  {
//					  if(dataMoleculesIni.get(i).getMolecula().startsWith("He"))
//						  dataMol.setMolecula("He");
//					  else if(dataMoleculesIni.get(i).getMolecula().startsWith("H"))
//						  dataMol.setMolecula("H");
//					  else if(dataMoleculesIni.get(i).getMolecula().startsWith("C"))
//						  dataMol.setMolecula("C");
//					  else if(dataMoleculesIni.get(i).getMolecula().startsWith("S"))
//						  dataMol.setMolecula("S");
//				  } else {

					  dataMol.setMolecula(dataMoleculesIni.get(i).getMolecula());
//				  }
				
				  try{
					  deltaN = new Integer(dataMoleculesIni.get(i).getDelta());
					  dataMol.setDelta(dataMoleculesIni.get(i).getDelta());
				  } catch (Exception e) {
					  deltaN = 2;
					  dataMol.setDelta("2");
				  }
					  
				  dataMolecules.add(dataMol);
			  }
		  } else if(params.getCriteriaDefault()!=null && !params.getCriteriaDefault().equals("")) 
		  {

			  String[] criteria =  MyUtilities.createArraysStringValues(params.getCriteriaDefault(),MyConstants.SEPARATOR_NIVEL_2);
				 
			  try{
				  if(criteria.length>=1)
				  {
					  deltaN = new Integer(criteria[0]);
				  } else {
					  deltaN = 2;												  
				  }
			  } catch (Exception e) {

				  deltaN = 2;
			  }
		  }

		  RangeH recombLineSearch;
		  Vector<String> recombResultsSpecies = new Vector<String>();
		  Vector<Object> recombResults = new Vector<Object>();
		  /*
		   * Checks the jump is not greater than 12 or smaller than 1
		   */		  
		  if(deltaN<1 || deltaN>12)
		  {
			  if(deltaN<1)
			  {
				  JOptionPane.showMessageDialog(searchPanel.getDialogResults(),MyConstants.LABEL_DELTA_NO_MAC+" has to be larger than 1. \n Value set to 2.","Search Error",JOptionPane.ERROR_MESSAGE);
				  deltaN = 2;
			  }
			  else
			  {
				  JOptionPane.showMessageDialog(searchPanel.getDialogResults(),MyConstants.LABEL_DELTA_NO_MAC+" is currently limited to 12. \n Value set to 12.","Search Error",JOptionPane.ERROR_MESSAGE);
				  deltaN = 12;
			  }
		  }
        /*
         * Calls to the getFrequencyRange function and uses the returned ArrayRange
         * to perform the different searchs.
         */
       // ArrayRange freqRanges = getFrequencyRangeSelected(params.getArrListRange(), params.getLabelAxis(), params.getLabelUnitAxis());
		       	
        try
        {
      	  //	LineSearchResultsTable tableCurrent = null;
      	  	String sForm = null;
      	  	String sN = null;

      	  	//HAY QVER PPRIMERO SI VOY A BUSCAR  O A GENERAR
      	  	//AUNQUE SE SUPONER QUE GENERAR LO HACE OTRO PLUGIN,. ASI QUE SE PUEDE QUITAR DE AQUI EL CODIGO DE GENERAR
      	  	//HAY QUE 
      	  	
//      	  	if(!params.isNew())
//      	  		tableCurrent = searchPanel.getDialogResults().getSelectedTable();
//        	int iColumnForm = -1;
//			if(tableCurrent!=null)
//			{
//				iColumnForm = tableCurrent.getResultSLIMDataJTable().findColumn(SlimConstants.COLUMN_FORMULA);
//			}
			
        	long [] resultSearch = null;
      	  /*
      	   * Order the frequencies and search
      	   */
      	  
	    	for(Range _range: freqRanges.getArrayCollapsed())
	    	{
	    		if (dataMolecules.size()>0)
	            {
	    			  for (int irow=0 ; irow<dataMolecules.size(); irow++)
	              	  {
	              		  sForm =  dataMolecules.get(irow).getMolecula();
	              		  
	              		  int delta = -1 ;
	              		  if(dataMolecules.get(irow).getDelta()!=null && !dataMolecules.get(irow).getDelta().equals(""))
	              			delta = new Integer(dataMolecules.get(irow).getDelta());
	              		  if(delta>12)
	              			  sN = "12";
	              		  else if(delta>0)
	              			  sN = dataMolecules.get(irow).getDelta();
	              		  else
	              			  sN = "2";
	    	    		  recombLineSearch = new RangeH();
//			    		  if(tableCurrent!= null)
//			    			  resultSearch = tableCurrent.searchViewRowInBaseTable(iColumnForm, sForm, sForm, null);
			    		  if(resultSearch==null || resultSearch.length<1)
	            		  {
			    			  
			    			
			    			  recombResults.add(
			    					  recombLineSearch.funcion(
			    							  Double.parseDouble(sN), 
			    							  sForm, 
			    							  Double.parseDouble(_range.getStart().toString())*1e6, 
			    							  Double.parseDouble(_range.getEnd().toString())*1e6
			    					  ));
			    			  recombResultsSpecies.add(sForm);
	            		  } 
	    			  }
	              } 
	    	  }
        } catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
        String [] nameFileMAdcube = new String[]{params.getAssociatedFile(), params.getTypeAssociatedFile()};
        
        //TODO NECESITA NUEVO NOMBRE CUANDO GENERA EL ESPECTRO
//        if(isAddFormule && recombResultsSpecies.size()<1)
//        	JOptionPane.showMessageDialog(searchPanel,"Formule/s are already added in table.","Search Error",JOptionPane.ERROR_MESSAGE);
//        else
        
//        EditorInformationMADCUBA.append("			    			  recombResults="+			    			  recombResultsSpecies);
       boolean isResult = searchPanel.getDialogResults().readRecombSearchResult(isSameProduct,criteriaSearch,recombResults, 
        			recombResultsSpecies, params.getSearchType(), nameFileMAdcube, isMacro,moleculesSearch, moleculesUpdate,  moleculesDelete);

    	if(isResult&&!isSameProduct)
		{
			   //CUIDADO AL BUSCAR, CUANDO SE ADD LOS PARAMS DEL RESULTADO
			   //NO DEBE CAMBIAR FILTER NI NOISE DEL RESTO, PERO SI DEL QUE HE ADD
			   //EN LA BUSQUEDA
//			  SlimUtilities.validateRMSFromSlim(nameFileMAdcube[0]);	
			   searchPanel.setNameFileMADCUBA(nameFileMAdcube, true, false);
			   //RMS SOLO SI YA NO SE HA PUESTO	        	
		}
       	return isResult;
    }
	
	/**
     * Gets the frequency ranges covered by the selected spectra.
	 * @param arrayList  is a Array
	 * @param labelAxis  is a String
	 * @param unit  is a String
     * @return ArrayRange 
     */	
    public static ArrayRange getFrequencyRangeSelected(ArrayList<Double> arrayList, String labelAxis, String unit)
    {
        ArrayRange freqArrayRange = new ArrayRange(); 
  	    for (int i=0; i <arrayList.size(); i+=2)
		{
			  double dMin = arrayList.get(i);
			  double dMax = arrayList.get(i+1);
			  //TODO CHANGE UNIT QUE DEBE SER FREQ EN MHz

			if(labelAxis!=null && labelAxis.equals("Frequency"))
			{
				  if(!unit.equals("MHz"))
				  {
	  				Unit unitOrig =  Unit.parse(unit.replace('_', ' '));		
	  				if(unit!=null)
	  				{
	  					dMin = AstronomicalChangeUnit.changeUnit(unitOrig,Frequency.MEGAHERTZ,dMin);
	  					dMax = AstronomicalChangeUnit.changeUnit(unitOrig,Frequency.MEGAHERTZ,dMax);
	  				}
				  }
			} else if(labelAxis!=null) {
				double[] values = SlimUtilities.unitConversion(new double[]{dMin, dMax}, unit, "Frequency");
				//RETURN IN MHz
				dMin =  values[0];
				dMax =  values[1];
			}
			freqArrayRange.addRange(new Range(dMin, dMax));
		}
  	    freqArrayRange.collapseRange();
  	    return freqArrayRange;
    }

    private static RowListStarTable generateStarTableCriteria(ArrayList<DataMolecular> dataMolecules, SearchSlimParams params, String[] nameFileMAdcube) 
	{
	    RowListStarTable rows = null;
	    ColumnInfo[] columnMolecules = SlimUtilities.generateHeaderSLIMSearch();
		rows = new RowListStarTable(columnMolecules);
		rows.setName("SINGLE_DISH");
		if(dataMolecules!=null && dataMolecules.size()>0)
		{
			for (int irow=0 ; irow<dataMolecules.size(); irow++)
			{
				Object[] row = new Object[columnMolecules.length];
				row[SlimConstants.POS_COLUMN_MOLEC_INDEX] = (irow+1);
				row[SlimConstants.POS_COLUMN_MOLEC_MOLECULE] = dataMolecules.get(irow).getMolecula();
				row[SlimConstants.POS_COLUMN_MOLEC_CATALOG] = dataMolecules.get(irow).getCatalog();
				//YA VEREMOS SI TENEMOS MARCA DE SQL
				//
				//20220113 poniendo mire en lista
				if(row[SlimConstants.POS_COLUMN_MOLEC_CATALOG]!=null 
						&&!row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].equals("SQL")
						&& Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,row[SlimConstants.POS_COLUMN_MOLEC_CATALOG])>=0)
//						&& (row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].equals(SlimConstants.CATALOG_JPL) 
//								|| row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].equals(SlimConstants.CATALOG_CDMS) 
//								|| row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].equals(SlimConstants.CATALOG_USER)))
				{
					String sElow = "ANY";
					if(dataMolecules.get(irow).getElow()!=null && !dataMolecules.get(irow).getElow().equals(""))
						sElow =  dataMolecules.get(irow).getElow()+"";
					String sCM = "ANY";
					if(dataMolecules.get(irow).getCm_1()!=null && !dataMolecules.get(irow).getCm_1().equals(""))
						sCM =  dataMolecules.get(irow).getCm_1() + "";
					String sLog = "ANY";
					if(dataMolecules.get(irow).getLog10()!=null && !dataMolecules.get(irow).getLog10().equals(""))
						sLog =  dataMolecules.get(irow).getLog10() + "";
					if(!sElow.toUpperCase().equals("ANY") && !sElow.equals(""))
						row[3] = sElow;
					if(!sCM.toUpperCase().equals("ANY") && !sCM.equals(""))
						row[4] = sCM;
					if(!sLog.toUpperCase().equals("ANY") && !sLog.equals(""))
						row[5] = sLog;				
				} else {
					row[3]=null;
					row[4]=null;
					row[5]=null;
				}
				if(row[SlimConstants.POS_COLUMN_MOLEC_CATALOG]!=null 
						&&!row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].equals("SQL")
						&&row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].toString().toUpperCase().contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase())   )
				{
					if(dataMolecules.get(irow).getDelta()!=null && !dataMolecules.get(irow).getDelta().equals(""))
						row[6] = dataMolecules.get(irow).getDelta()+"";
				} else {
					row [6] = null;
				}
				if(row[SlimConstants.POS_COLUMN_MOLEC_CATALOG]!=null 
						&&!row[SlimConstants.POS_COLUMN_MOLEC_CATALOG].equals("SQL"))
				{
					String rangeSearch = "";
						
					for(int iRange=0; iRange<params.getArrListRange().size(); iRange++)
		            {
		            	if(params.getLabelAxis()!=null && params.getLabelAxis().equals("Frequency"))
		            	{
			        		if(!params.getLabelUnitAxis().equals("Hz"))
			        		{
				    			Unit unit =  Unit.parse(params.getLabelUnitAxis().replace('_', ' '));		
				    			if(unit!=null)
				    			{
				    				rangeSearch += AstronomicalChangeUnit.changeUnit(unit,Frequency.HERTZ,params.getArrListRange().get(iRange)) + MyConstants.SEPARATOR_NIVEL_1;
				    			}			    			
			        		}
		            	}else if(params.getLabelAxis()!=null) {
		            		double value = SlimUtilities.unitConversion(params.getArrListRange().get(iRange), params.getLabelUnitAxis(), "Frequency");
		            		//RETURN IN MHz
		            		value =  Frequency.MEGAHERTZ.getValue(value, Frequency.HERTZ);
		            		rangeSearch += value + MyConstants.SEPARATOR_NIVEL_1;
		            	}
		            }
					row [SlimConstants.POS_COLUMN_MOLEC_RANGE_WHERE] = rangeSearch;
					row [10] = "Frequency";
				} else {

					row [SlimConstants.POS_COLUMN_MOLEC_RANGE_WHERE] = null;
					row [10] = null;
				}
				//FILE ANS TYPE FILE
				String[] nameTypeFile = nameFileMAdcube; 
	//			Log.getInstance().logger.debug(nameTypeFile[0]+"--------"+nameTypeFile[1]);
				if(nameTypeFile[0]!=null && nameTypeFile[0].toLowerCase().lastIndexOf(".fits")>-1)
					row [SlimConstants.POS_COLUMN_MOLEC_FILE_MADCUBAIJ] = nameTypeFile[0].substring(0,nameTypeFile[0].toLowerCase().lastIndexOf(".fits")+5);
				else
					row [SlimConstants.POS_COLUMN_MOLEC_FILE_MADCUBAIJ] = nameTypeFile[0];
				row [SlimConstants.POS_COLUMN_MOLEC_TYPE_FILE] = nameTypeFile[1];
				row[SlimConstants.POS_COLUMN_MOLEC_RESOLUTION] = null;
		    	
				row[SlimConstants.POS_COLUMN_MOLEC_BEAMSIZE] = null;
				if(params.getNameTAB()!=null && !params.getNameTAB().equals(""))
					row[SlimConstants.POS_COLUMN_MOLEC_NAME_TAB] = params.getNameTAB();
				else
					row[SlimConstants.POS_COLUMN_MOLEC_NAME_TAB] = null;
				
				if(dataMolecules.get(irow).getWHERE()!=null)
					row[SlimConstants.POS_COLUMN_MOLEC_WHERE_EXTRA] = dataMolecules.get(irow).getWHERE();
				else
					row[SlimConstants.POS_COLUMN_MOLEC_WHERE_EXTRA] = null;
				
				if(dataMolecules.get(irow).getRenameMolecule()!=null 
						&& !dataMolecules.get(irow).getRenameMolecule().isEmpty() 
						&& !dataMolecules.get(irow).getRenameMolecule().toLowerCase().equals("none"))
					row[SlimConstants.POS_COLUMN_MOLEC_RENAME_MOLECULE] = dataMolecules.get(irow).getRenameMolecule();
				else
					row[SlimConstants.POS_COLUMN_MOLEC_RENAME_MOLECULE] = null;
				rows.addRow(row.clone());
			}
		} 
		return rows;
    }
    
	public static SearchSlimParams getParamsPlugin(String sArg) {
	    SearchSlimParams params = null;
	    if(sArg != null && !sArg.equalsIgnoreCase(""))
		{
			params = new SearchSlimParams();	
			//PRIMERO MIRAMOS SI VIENE DIRECTAMENTE LA SQL, porque entonces hay params que no 
			//tiene que coger porque no va a generar la SQL, asi sin si querer se han puesto, 
			//lo ignora
			params.setSQL (Macro.getValue(sArg+ " ",  "sql", null));
			boolean isSQL = params.getSQL()!=null && !params.getSQL().isEmpty();

			String fileSql = Macro.getValue(sArg+ " ",  "inputsql", null);
			if(fileSql!=null)
			{
				isSQL=true;
				ImportFileSimulate importSimulate = new ImportFileSimulate();
				boolean isOK = importSimulate.importParamsSQL(fileSql, params);
				if(isOK )
				{
					params.setFileSQL(fileSql);
					isSQL =true;
				} 
			}
			if(!isSQL)
			{
				params.setListRange(Macro.getValue(sArg+ " ",  "range", null));
	//			Log.getInstance().logger.debug(sArg);
				//AHORA VEMOS LOS DIFERENTES DATOS DEL RANGE
				boolean isWholeRange = false;
				if(params.getListRange()!=null 
						&& params.getListRange().toLowerCase().equals(SELECTED_WHOLE_RANGE))
				{
					ArrayList<Double> rangeList = new ArrayList<Double>();
					rangeList.add(0D);
					rangeList.add(Double.MAX_VALUE);//OR 300000 UN PAR DE CEROS MAS
					params.setArrListRange(rangeList);
					params.setLabelAxis("Frequency");
					params.setLabelUnitAxis("Hz");
					isWholeRange = true;
				} else if(params.getListRange()!=null 
						&& !params.getListRange().toLowerCase().equals(SELECTED_WINDOW)
						&&!params.getListRange().toLowerCase().equals(SELECTED_DATA)
						&&!params.getListRange().toLowerCase().equals(CUBE_CONTAINER))
				{
					params.setArrListRange(MyUtilities.createArrayListDoublesValues(params.getListRange()));
				} else if(params.getListRange()==null) {
					params.setListRange(SELECTED_DATA);
				}
			
				
				if(!isWholeRange)
				{
					params.setLabelAxis(Macro.getValue(sArg+ " ",  "axislabel", null));
					String sLabelUnit = Macro.getValue(sArg+ " ",  "axisunit", "--");
					params.setLabelUnitAxis(sLabelUnit);
				}
				
				
				params.setListMolecules(Macro.getValue(sArg+ " ",  "molecules", null));
				params.setArrListMolecules(MyUtilities.createArraysStringValues(params.getListMolecules(),MyConstants.SEPARATOR_NIVEL_1) );
				
				params.setCriteriaDefault(Macro.getValue(sArg+ " ", "criteria", null));	
				
			}	else {
				//TENGO QUE VER SI SACO DE AQUI LAS MOLECULAS DE BUSQUEDA O NO 
//				params.setListMolecules(Macro.getValue(sArg+ " ",  "molecules", null));
//				params.setArrListMolecules(MyUtilities.createArraysStringValues(params.getListMolecules(),MyConstants.SEPARATOR_NIVEL_1) );
		
			}
			
			params.setSearchType(Macro.getValue(sArg+ " ",  "searchtype", "new"));
			params.setAssociatedFile(Macro.getValue(sArg+ " ", "datafile", null));
			params.setTypeAssociatedFile(Macro.getValue(sArg+ " ", "datatype", null));
			params.setClear(new Boolean(Macro.getValue(sArg+ " ",  "clearsearch", "false")));

			params.setWHERE(Macro.getValue(sArg+ " ",  "where", null));
			if(params.getWHERE()!=null && !params.getWHERE().isEmpty())
				params.setWHERE("("+params.getWHERE().replace('$', '\'')+")");
			params.setNewNameMolecule(Macro.getValue(sArg+ " ",  "rename_molecule", null));
		}
	    
	    return params;
    }
	
	public static String replaceStringCatalog(String cadena, String valueReplaceIn, String valueReplaceOut)
	{
		String cadenaReturn = "";

//      	Log.getInstance().logger.debug("_replaceStringCatalog___"+cadena+"___"+valueReplaceIn+"____"+valueReplaceOut+"_____");
		if(valueReplaceIn.toUpperCase().equals(SlimConstants.CATALOG_CDMS))
		{
			int index = cadena.indexOf(valueReplaceIn);

			String cadenaReturnTemp = cadena;
			while (index>=0)
			{
//				int indexTemp = cadenaReturnTemp.toUpperCase().indexOf(SlimConstants.CATALOG_CDMSHFS);
				int indexTempU = cadenaReturnTemp.indexOf(SlimConstants.CATALOG_CDMSHFS);
				int indexTempL = cadenaReturnTemp.indexOf(SlimConstants.CATALOG_CDMSHFS.toLowerCase());
				
				int indexTempA = cadenaReturnTemp.indexOf(SlimConstants.CATALOG_CDMSOP);
				int indexTempO = cadenaReturnTemp.indexOf(SlimConstants.CATALOG_CDMSOP.toLowerCase());
//				System.out.println(indexTempL+"="+index+"="+indexTempU);
				if(indexTempU==index || indexTempL==index)
				{
//				if(indexTemp==index)
//				{
					cadenaReturn += cadenaReturnTemp.substring(0,index+valueReplaceIn.length());
				
				} 
				// Eduardo Toledo Dec 2024
				// Adding the handling of ortho and para catalog for CDMS
				else if (indexTempA==index || indexTempO==index) 
				{
					cadenaReturn += cadenaReturnTemp.substring(0,index+valueReplaceIn.length());
					
				} else {
					cadenaReturn += cadenaReturnTemp.substring(0,index)+valueReplaceOut;	
					
				}
				
				
				
				if(index+valueReplaceIn.length()<cadenaReturnTemp.length())
					cadenaReturnTemp = cadenaReturnTemp.substring(index+valueReplaceIn.length());
				else
					cadenaReturnTemp = "";
				index = cadenaReturnTemp.indexOf(valueReplaceIn);
			}
			cadenaReturn +=cadenaReturnTemp;
		} else {
			cadenaReturn = cadena.replace(valueReplaceIn, valueReplaceOut);
		}

//		System.out.println(cadenaReturn+"=Final");
//		Log.getInstance().logger.debug(cadenaReturn+"=Final");
		
		return cadenaReturn;
	}

	public static boolean existsResult ()
	{
		
		return isFoundNewValues;
	}
    
	private String getInfo() {
			// TODO Auto-generated method stub
		String info ="List params SLIM_SEARCH:\n";
		info += "  **  range = this range is frequency list or wavelength list. The arguments can be: \n";
		info += "    --The list  separate with '#': freq1#freq2#..\n";
	 	info += "    --selected_data: Make search about range selected frequency in associated data spectra(selected rows)/cube  \n";
	 	info += "    --window_active: Look the active plot from associated product and it uses the range shows in this plot\n";
	 	info += "    --whole: Make search about all possible frequencies (0-"+Double.MAX_VALUE+"). \n";
		info += "  **  axislabel = It is the label of type of unit (xAxis) showing (range). The types are:frequency,wavelength\n";
	 	info += "  **  axisunit = It is the label of type of selected unit.  \n";
	 	info += "    --If the unit label is frequency then it is Hz, MHz, GHz, TGHz  \n";
	  	info += "    --If the unit label is wavelength thin it is cm, mm, micrometer, nm, ua\n";
	  	info += "  **  molecules = It is the list of molecules in the next format:\n";
	  	info += "    --CASE 1: CATALOG$MOLECULE$CRITERIOS#CATALOG$MOLECULE$CRITERIOS#. Ej; JPL$CS$Any$-3$Any$300#CMDS$CS+$Any$Any$Any@300#\n";
	  	info += "    --CASE 2: CATALOG$MOLECULE#. Ej1: JPL$C*#.   Ej2: lovas1992$CS#\n";
	  	info += "    --CASE 3: CATALOG$MOLDECULE$DELTA#. Ej: Recomb$C$1#\n";
	  	info += "  **  searchtype = the arguments are:\n";
	  	info += "    --new:  it makes a new search in a new tab (DEFAUTL PARAMS)\n";
	  	info += "    --add: the result is added to last search.\n";
	  	info += "    --update_transitions: In a tab, repeat a search and add new transitions (you can change the frequency range)\n";
	  	info += "  **  datafile = It is the name of associated MADCUBA file\n";
	  	info += "  **  datatype = It is the type of associated MADCUBA file. It can be SPECTRA OR CUBE (if not exist no problem)\n";
	 	info += "  **  searchcriteria = list default values molecules,  if it not exists any molecule criteria then searched filters with this criteria (format delta_n$ElowMin$ElowMax$logI)\n";
	  	info += "  **  clearsearch = It is true or false. If the parameter doesn't exist then the value is false \n";
	 	info += "    --It the parameter is true then the table is empty and the directory with all search files. Too the variables of tab used to search\n";
	 	info += "    --It the parameter is false then no make nothing new. (make a search normal)\n";
		info += "Method existsResult. Indicated it is found some transitiomn in las Search. Return true or false. Example: call(\"SLIM_Search.existsResult\")";
		return info;
	}
}
