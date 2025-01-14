/**
 * $Id $
 *
 *              Copyright (c) 2006
 *              Sergio Martin Ruiz, Madrid, Spain
 */

package es.smr.slim;

import herschel.share.unit.Speed;
import herschel.share.unit.Unit;
import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.io.OpenDialog;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;

import es.cab.astronomical.AstronomicalChangeUnit;
import es.cab.astronomical.AstronomicalImagePlus;
import es.cab.astronomical.AstronomicalPlotWindow;
import es.cab.astronomical.components.AstronomicalJFrameMASSA;
import es.cab.astronomical.components.AstronomicalJTableMASSA;
import es.cab.astronomical.utils.AstronomicalFunctionsGenerics;
import es.cab.madcuba.log.Log;
import es.cab.madcuba.tools.PropertiesLastConfig;
import es.cab.madcuba.utils.MyConstants;
import es.cab.madcuba.utils.MyUtilities;
import es.cab.plugins.ListPluginsNames;
import es.cab.plugins.SynchronizeCubePlugin;
import es.cab.swing.gui.EditorInformationMADCUBA;
import es.smr.slim.beans.SearchSlimParams;
import es.smr.slim.components.PanelListMolecules;
import es.smr.slim.components.qnlte.SimFitCollisionMoleculePanel;
import es.smr.slim.components.qnlte.SimFitSourceParametersPanel;
import es.smr.slim.plugins.ListPluginsNamesSlim;
import es.smr.slim.plugins.SLIMFRAME;
import es.smr.slim.plugins.SLIMSearch;
import es.smr.slim.utils.SlimConstants;
import es.smr.slim.utils.SlimUtilities;
import es.smr.slim.utils.SlimUtilitiesFiles;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class creating the GUI to access the database.<br>
 * This class allows:<br>
 * - Catalog Selection<br>
 * - Frequency/Wavelenght search constraint<br>
 * - Additional selection criteria for each particular catalog
 * 
 * @author Sergio Martin Ruiz
 * 
 */
@SuppressWarnings("serial")
public class LineSearchPanel extends JPanel implements KeyListener {
	public static final String SELECTED_DATA = "selected_data";
	public static final String SELECTED_WINDOW = "selected_window";
	public static final String SELECTED_WHOLE_RANGE = "whole";
	public static final String CUBE_CONTAINER = "cube_container";
	private static final String COMBO_HZMM_GENERATE = "HzmmGenerate";
	private static final String COMBO_HZMM_CRITERIA = "HzmmCriteria";
	public static final int RANGE_USER_DEFINED = 2;
	public static final int RANGE_WINDOW = 1;
	public static final int RANGE_WHOLE_FREQ = 3;
	/*
	 * Creates a LineSearchResults Window Invisible until Search Button is pressed
	 */
	public LineSearchResultsDialog dialogResults;
	private int panelWidth = 850;

	/*
	 * The Database Object and the Array of ResultSets
	 */
	private static DbHSQLDBCreate db = null;

	protected static Log _log = Log.getInstance();

	// CONSTANST
	/*
	 * Panels defined: - Search Button - Catalog Selection Panel - Frequency
	 * Selection Panel - Criteria Panel
	 */

	private JButton bOKJPL = null;
	private JButton bOKRecom = null;
	/*
	 * private JButton bLineSearch = new JButton(); private JButton bClearSearch =
	 * new JButton(); private JRadioButton radioNew = new JRadioButton("New");
	 * private JRadioButton radioAddCurrent = new JRadioButton("Add Current");
	 * private JPanel jPanelUpperButtons = new JPanel();
	 */
	private JPanel jPanelFrequency = new JPanel();
	private JPanel jPanelCriteria = new JPanel();
	private JPanel jPanelCriteriaDefault = null;
	private JPanel jPanelCriteriaJPLCDMS = null;
	private JPanel jPanelCriteriaRECOMB = null;
	private JPanel jPanelGenerateSpectra = new JPanel();
	private SimFitCollisionMoleculePanel jPanelCollisionMolecule = null;
	private SimFitSourceParametersPanel jPanelParametersSource = null;

	/*
	 * Definition of components of Panel Catalog
	 */
	private static PanelListMolecules jpListMolecules = null;// new PanelListMolecules();

	/*
	 * Definition of components of Panel Frequency
	 */
	private JPanel jpRangeCriteria = new JPanel();
	private JPanel jpGenerateSpectraCriteria1 = new JPanel();
	private JPanel jpGenerateSpectraCriteria2 = new JPanel();
	private JPanel jpGenerateSpectraCriteria3 = new JPanel();
	private static JComboBox selectTypeRange = new JComboBox();
	// private static JCheckBox checkBoxFreq = new JCheckBox("Auto", true);
	// private static JCheckBox checkBoxWindow = new JCheckBox("Window", false);
	private static JTextField tfBoxNoise = new JTextField(3);
	private static JComboBox cBoxNuLambda = new JComboBox();
	private static JComboBox cBoxNuLambdaGenerateSpectra = new JComboBox();
//	private static JLabel lVeloSpectral = new JLabel("");
	private static JLabel lFreqMin = new JLabel("From");
	private static JLabel lFreqMax = new JLabel("To");
	private static JTextField tfFreqMin = new JTextField(5);
	private static JTextField tfFreqMax = new JTextField(5);
	private static JTextField tfFreqMinGenerate = new JTextField(5);
	private static JTextField tfFreqMaxGenerate = new JTextField(5);
	private static JLabel lResolution = new JLabel("LineWidth");
//	private static JTextField tfResolutionMin = new JTextField(5);
//	private static JTextField tfResolutionMax = new JTextField(5);
	private static JTextField tfLineWidth = new JTextField(5);
//	private static JLabel lRestFrequency = new JLabel("Rest.Freq/Wave");
	//private static JTextField tfRestFrequency = new JTextField(5);
	private static JLabel lBeamSize = new JLabel("Beam Size");
	private static JTextField tfBeamSize = new JTextField(3);
	private JComboBox cBoxHzmm = new JComboBox();
	private JComboBox cBoxHzmmGenerateSpectra = new JComboBox();
	private JComboBox cBoxVelocityGenerateSpectra = new JComboBox(new String[] { "m/s", "km/s" });
	private int cBoxNuLambdaSelected;
	private int cBoxHzmmSelected;
	private int cBoxNuLambdaGenerateSelected;
	private int cBoxHzmmGenerateSelected;
	private JComboBox cBoxUnitsIntesity = new JComboBox(new String[] { "K", "Jy" });
	private JComboBox cBoxTempscalIntensity = new JComboBox(new String[] { "TMB", "TA*" });

	private final String[] arrayHz = new String[] { "THz", "GHz", "MHz", "Hz" };
	private final double[] arrayHzFactor = new double[] { 1000000, 1000, 1, 0.000001 };
	private final int FREQ_DEFAULT_COMBO = 2; // Default scale of Frequency
	private final int FREQ_HZ_POS_COMBO = 3; // Where Hz is the array arrayHz (used for conversions)
	private final int FREQ_MHZ = 2;
	private final String[] arrayMm = new String[] { "cm", "mm", "\u03BCm", "nm", "\u00C5" };
	private final double[] arrayMmFactor = new double[] { 10000, 1000, 1, 0.001, 0.0001 };
	private final int WAVE_DEFAULT_COMBO = 1; // Default scale of Wavelenght
	private final int WAVE_CM_POS_COMBO = 0; // Where cm is in the array arrayMm (used for conversions)
	private final int WAVE_MM_DEFAULT = 1;

	/*
	 * Definition of components of Panel Criteria
	 */
	private JPanel jpCritLine1 = new JPanel();
	private JPanel jpCritLine2 = new JPanel();
	// DEFAULT PANEL
	private JLabel lInitText;
	// JPL & CDMS
	private JLabel lEnergy;// = new JLabel("<html><i>E</i><sub>low</sub> </html>"); //Lower level energy
							// range ");
	private JLabel lEnergyunits;// = new JLabel("<html>cm<sup>-1</sup> </html>");
	private JLabel lIntensity;// = new JLabel("<html>log<sub>10</sub>(I)</html>");
	private JLabel lIntensityAt;// = new JLabel("@");
	private JLabel lIntensityK;// = new JLabel("K");
	private JTextField tfEnergy;// = new JTextField(5);
	private JTextField tfEnergy2;// = new JTextField(5);
	private JTextField tfIntensity;// = new JTextField(5);
	private JTextField tfIntensityT;// = new JTextField(5);
	// RECOMBINATION LINES
	private JLabel lDeltaN;
	private JTextField tfDeltaN;

	// RANGE SEARCH
	private String rangeSearch = null;
	private String[] nameFileMADCUBA = new String[2];

	// FORMAT
	private DecimalFormat format = null;
	private DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);

	boolean isMakeEventChangeNuLambda = true;
	boolean isMakeEventChangeNuLambdaGenerate = true;
	boolean isMakeEventChangeHzmm = true;
	boolean isMakeEventChangeHzmmGenerate = true;

	/**
	 * Class Constructor:<br>
	 * 
	 * @param PathDB -- String Absolute path to DB.
	 * @param lsrd   -- LineSearchResultsDialog
	 */
	public LineSearchPanel(String PathDB, LineSearchResultsDialog lsrd) {
		Log.getInstance();
		initDBConnection(PathDB);
//
//		Log.getInstance().logger.debug(System.getProperty("user.dir")+"=USERDIR no");
//		Log.getInstance().logger.debug(PathDB+"=PathDB");
		initGUI();
		dialogResults = lsrd;

		initialsParams();
	}

	/**
	 * Default Class Constructor:<br>
	 * The DataBase is assumed to be located where the program is running
	 */
	public LineSearchPanel(LineSearchResultsDialog lsrd) {
		Log.getInstance();
		Log.getInstance().logger.debug(System.getProperty("user.dir")+"=USERDIR");
		initDBConnection(System.getProperty("user.dir") + "/"+MyConstants.PATH_DIRECTORY_CATALOG + SlimConstants.NAME_FILES_BD);
		initGUI();
		dialogResults = lsrd;
		if (isMADCUBA_IJconnected()) {
			selectTypeRange.setSelectedIndex(0);
		}
	}

	/**
	 * Connects to the database
	 */
	private void initDBConnection(String databasePath) {
		try {
			// _log.setLevel(Level.WARN);
			_log.logger.debug(" CreateDbHSQLDB: " + databasePath);
			db = new DbHSQLDBCreate(databasePath);
		} catch (Exception ex) {
			Log.getInstance().logger.error("Error initDBConnection ");
			ex.printStackTrace();
		}
	}

	/**
	 * Filling Combo Box with available Catalogs from the DataBase<br>
	 * Long names of Catalogs are read from the TableIndex in DataBase
	 * 
	 * @param args Selected Type of Catalog to Search
	 */
	public void fillcBoxCatalog(String args) {
		ResultSet dbresult = null;
		String sqlcommand;

		sqlcommand = "SELECT id_tablelongname FROM TableIndex";
		if (!args.toUpperCase().equals("ALL"))
			sqlcommand = sqlcommand.concat(" WHEREd id_tabletype = \"").concat(args).concat("\"");
		try {
			dbresult = db.query(sqlcommand);
			jpListMolecules.getComboCatalog().removeAllItems();
			if(db.db_exists(false))
				for (; dbresult.next();) {
					jpListMolecules.getComboCatalog().addItem(dbresult.getObject(1).toString());
				}
			

		} catch (SQLException ex3) {
			Log.getInstance().logger.error("Error fillcBoxCatalog ");
			ex3.printStackTrace();
		} catch (NullPointerException ex) {
			_log.logger.warn("Database not available: " + "" + ex.getMessage());
		}
		try {
			
			// Check if USER DB exists. If so, add the USER inputs (currently there should be only one.
			if(db.st_user!=null && db.db_exists(true)){
				dbresult = db.query(true,sqlcommand);
				for (; dbresult.next();) {
					jpListMolecules.getComboCatalog().addItem(dbresult.getObject(1).toString());
				}
			}

		} catch (SQLException ex3) {
			Log.getInstance().logger.error("Error fillcBoxCatalog ");
			ex3.printStackTrace();
		} catch (NullPointerException ex) {
			_log.logger.warn("Database not available: " + "" + ex.getMessage());
		}

		jpListMolecules.getComboCatalog().addItem("Recomb. Lines");
	}

	/**
	 * Filling Combo box with available Molecules/Atoms in the DataBase according to
	 * the Catalog selected.<br>
	 * - Allows writing in the Combo Box to constraint the species shown when
	 * ComboBox is opened<br>
	 * - Species are ordered alphabetically
	 * 
	 * @param args Selected Species or string contained in the species name
	 */
	public void fillcBoxSpecies(String args) {
		// Temporary solution to add the Recombination lines

		if (jpListMolecules.getComboCatalog().getSelectedItem().equals("Recomb. Lines")) {
			jpListMolecules.getListModelIn().removeAllElements();
			jpListMolecules.getListModelIn().addElement("H");
			jpListMolecules.getListModelIn().addElement("He");
			jpListMolecules.getListModelIn().addElement("C");
			jpListMolecules.getListModelIn().addElement("S");
		} else {
			////// REMOVE THISSSS
			jpListMolecules.getButtonLineSearch().setEnabled(true);
			/////
			ResultSet dbresult = null;
			String sqlcommand;
			try {
				/*
				 * First gets the name of the table in DB corresponding to the Long Name
				 * selected in the Catalog Combo Box
				 */
				sqlcommand = "SELECT id_tablename FROM TableIndex WHERE id_tablelongname = '"
						.concat(jpListMolecules.getComboCatalog().getSelectedItem().toString()).concat("'");
				dbresult = db.query(sqlcommand);
				dbresult.next();
				String row = dbresult.getObject(1).toString();
				/*
				 * If JPL, changes JPL to JPLcat (same with CDMS and USER), where the list of
				 * molecules is located
				 */
				
//				row = row.replace((String) SlimConstants.CATALOG_JPL, (String) "JPLcat");
//				row = row.replace((String) SlimConstants.CATALOG_CDMS, (String) "CDMScat");
//				row = row.replace((String) SlimConstants.CATALOG_USER, (String) SlimConstants.CATALOG_USER+"cat");
//				Log.getInstance().logger.debug(row+"="+Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, row));
				if(Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, row)>=0)
					row = row+"cat";
//				Log.getInstance().logger.debug(row+"=2="+Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, row));
				
				sqlcommand = "SELECT DISTINCT id_formula FROM ".concat(row);
				/*
				 * If selected something different to ALL or [empty], the combo will only
				 * display the species whose name contain the string written by the user.
				 */
				if (!args.toUpperCase().equals("ALL") && !args.equals(""))
					sqlcommand = sqlcommand.concat(" WHERE id_formula LIKE '%" + args.replace("*", "") + "%' ");
				sqlcommand = sqlcommand.concat(" ORDER BY id_formula ASC");
				// System.out.println(sqlcommand);
				dbresult = db.query(sqlcommand);
				// cBoxSpecies.removeAllItems();
				// cBoxSpecies.addItem("ALL"); // ALL always at the beginning of the list
				/*
				 * Filling of the Combo box with the search results
				 */
				/*
				 * for( ; dbresult.next() ; ){
				 * cBoxSpecies.addItem(dbresult.getObject(1).toString()); }
				 * cBoxSpecies.setSelectedItem(args); cBoxSpecies.setAutocomplete();
				 */
				jpListMolecules.getListModelIn().removeAllElements();
				for (; dbresult.next();) {
					// Eduardo Toledo Dec 2024 
					// Traza para depurar problema con nuevas moleculas
					//String value = dbresult.getObject(1).toString();
				    //System.out.println("Value: " + value);
				    //
					jpListMolecules.getListModelIn().addElement(dbresult.getObject(1).toString());
					
				}
			} catch (SQLException ex3) {
				Log.getInstance().logger.error("Error fillcBoxSpecies ");
				ex3.printStackTrace();
			}
		}
		jpListMolecules.autoFilter();
	}

	/**
	 * Filling Combo Box with the units of Frequency or Wavelength
	 * 
	 * @param items   Array of values
	 * @param selItem Integer of the position of the selected item in the array to
	 *                be displayed
	 * @param cBoxHzmm  JComboBox
	 */
	public void fillcBoxHzmm(String[] items, int selItem, JComboBox cBoxHzmm) {
		cBoxHzmm.removeAllItems();
		int i;
		for (i = 0; i < items.length; i++)
			cBoxHzmm.addItem(items[i]);

		cBoxHzmm.setSelectedIndex(selItem);
		if (cBoxHzmm.getName().equals(COMBO_HZMM_CRITERIA))
			cBoxHzmmSelected = selItem;
		else if (cBoxHzmm.getName().equals(COMBO_HZMM_GENERATE))
			cBoxHzmmGenerateSelected = selItem;
	}

	public void fillcBoxHzmmFreqCriteriaDefault() {
		fillcBoxHzmmFreqDefault(cBoxHzmm);
	}

	public void fillcBoxHzmmFreqGenerateDefault() {
		fillcBoxHzmmFreqDefault(cBoxHzmmGenerateSpectra);
	}

	public void fillcBoxHzmmFreqDefault(JComboBox cBoxHzmm) {
		fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmm);
	}

	public void fillcBoxHzmmWaveCriteriaDefault() {
		fillcBoxHzmmWaveDefault(cBoxHzmm);
	}

	public void fillcBoxHzmmWaveGenerateDefault() {
		fillcBoxHzmmWaveDefault(cBoxHzmmGenerateSpectra);
	}

	public void fillcBoxHzmmWaveDefault(JComboBox cBoxHzmm) {
		fillcBoxHzmm(arrayMm, WAVE_DEFAULT_COMBO, cBoxHzmm);
	}

	/**
	 * Creates the Default Selection Criteria Panel. Displays a text with no options
	 * 
	 * @param cataSelected Selected Catalog
	 */
	private JPanel createJPanelCriteriaDefault(String cataSelected) {
		if (jPanelCriteriaDefault == null) {
			jPanelCriteriaDefault = new JPanel();
			lInitText = new JLabel("No Search Criteria for Catalog " + cataSelected);
			jPanelCriteriaDefault.add(lInitText);
			// lInitText.setVisible(true);
		}
		return jPanelCriteriaDefault;

	}

	/*
	 * JPL - Posibility to select in: - Energies of lower level (cm-1) - Intensities
	 * (For different Temperatures)
	 */
	public JPanel createJPanelCriteriaJPLCDMS(String cataSelected) {
		if (jPanelCriteriaJPLCDMS == null) {
			jPanelCriteriaJPLCDMS = new JPanel();
			jpCritLine1 = new JPanel();
			jpCritLine2 = new JPanel();

			lEnergy = new JLabel("<html><i>E</i><sub>low</sub> </html>"); // Lower level energy range ");
			lEnergyunits = new JLabel("<html>cm<sup>-1</sup> </html>");
			lIntensity = new JLabel("<html>log<sub>10</sub>(I)</html>");
			lIntensityAt = new JLabel("@");
			lIntensityK = new JLabel("K");
			tfEnergy = new JTextField(5);
			tfEnergy2 = new JTextField(5);
			tfIntensity = new JTextField(5);
			tfIntensityT = new JTextField(5);

			jpCritLine1.add(new JLabel("Energy from"));
			jpCritLine1.add(tfEnergy);
			tfEnergy.setText("Any");
			jpCritLine1.add(new JLabel("to"));
			jpCritLine1.add(tfEnergy2);
			tfEnergy2.setText("Any");
			jpCritLine1.add(lEnergyunits);

			jpCritLine2.add(lIntensity);
			jpCritLine2.add(tfIntensity);
			jpCritLine2.add(lIntensityAt);
			jpCritLine2.add(tfIntensityT);
			jpCritLine2.add(lEnergy);
			jpCritLine2.add(lIntensityK);
			if (bOKJPL == null)
				createButtonOkJPL();
			jpCritLine2.add(bOKJPL);

			tfIntensity.setText("Any");
			tfIntensityT.setText("300");
			tfIntensityT.setEditable(false);

			jPanelCriteriaJPLCDMS.add(jpCritLine1);
			jPanelCriteriaJPLCDMS.add(jpCritLine2);
		}
		return jPanelCriteriaJPLCDMS;
	}
	
	public SimFitCollisionMoleculePanel createJPanelCollisionCatalog() {
		if (jPanelCollisionMolecule == null) {
			jPanelCollisionMolecule = new SimFitCollisionMoleculePanel(false);
	  	  	int panelHeight = 50;
	  	  	jPanelCollisionMolecule. setPreferredSize(new Dimension(panelWidth, panelHeight));
	  	  	jPanelCollisionMolecule. setMinimumSize(new Dimension(panelWidth, panelHeight));
	  	  	jPanelCollisionMolecule.  setMaximumSize(new Dimension(panelWidth, panelHeight));
//			
		}
		return jPanelCollisionMolecule;
	}
	public JPanel createJPanelParametersSource() {
		
		JPanel jPanelParameters= new JPanel();
//		if (jPanelParametersSource == null) {
			jPanelParametersSource = new SimFitSourceParametersPanel(false,true,true);
	  	  	int panelHeight = 100;
	  	  jPanelParametersSource. setPreferredSize(new Dimension(panelWidth, panelHeight));
	  	  jPanelParametersSource. setMinimumSize(new Dimension(panelWidth, panelHeight));
	  	  jPanelParametersSource.  setMaximumSize(new Dimension(panelWidth, panelHeight));
//		} 	
	  	jPanelParameters.setBorder(javax.swing.BorderFactory.createTitledBorder(
				javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 1), "SOURCE PARAMETERS",
				javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
				new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11), new java.awt.Color(60, 60, 60)));
	
	  	  	
	  	  	
  	  JButton bParametersSource = new JButton("Source Parameters");
  	  bParametersSource.setActionCommand("showPrameters");
  	  // bSyntheticSpectra.setEnabled(false);
  	  ActionListener alGenerateSpectra = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (ev.getActionCommand() != null && ev.getActionCommand().equals("showPrameters")) {
					panelSourceParametersVisible(!jPanelParametersSource.isVisible());

				} 
			}
		};
		bParametersSource.addActionListener(alGenerateSpectra);
		jPanelParameters.add(bParametersSource);
		jPanelParameters.add(jPanelParametersSource);
		panelSourceParametersVisible(false);
		return jPanelParameters;
	}
	
	private void createButtonOkJPL() {
		bOKJPL = new JButton("OK");
		bOKJPL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jpListMolecules.copyCriteriaCatalogToTable();
			}
		});
	}

	private void createButtonOkRecomb() {
		bOKRecom = new JButton("OK");
		bOKRecom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				jpListMolecules.copyCriteriaCatalogToTable();
			}
		});
	}

	public JPanel createJPanelCriteriaRECOMB(String cataSelected) {
		if (jPanelCriteriaRECOMB == null) {
			jPanelCriteriaRECOMB = new JPanel();
			jpCritLine1 = new JPanel();

			if (IJ.isMacintosh() || IJ.isMacOSX())
				lDeltaN = new JLabel(
						"Max. " + SlimConstants.COLUMN_DELTA_n_RECOMB_MAC + MyConstants.LABEL_MINOR_EQUAL_MAC);
			else
				lDeltaN = new JLabel(
						"Max. " + SlimConstants.COLUMN_DELTA_n_RECOMB + MyConstants.LABEL_MINOR_EQUAL_NO_MAC);
			tfDeltaN = new JTextField(2);
			jpCritLine1.add(lDeltaN);
			jpCritLine1.add(tfDeltaN);

			jpCritLine2 = new JPanel();

			if (bOKRecom == null)
				createButtonOkRecomb();

			jpCritLine2.add(bOKRecom);
			tfDeltaN.setText("2");

			jPanelCriteriaRECOMB.add(jpCritLine1);
			jPanelCriteriaRECOMB.add(jpCritLine2);
		}
		return jPanelCriteriaRECOMB;
	}

	/**
	 * Creates the Selection Criteria Panel. Only for selected catalogs, otherwise
	 * empty.
	 * 
	 * @param cataSelected Selected Catalog
	 */
	public void fillPanelCriteria(String cataSelected) {
		jPanelCriteria.removeAll();
		jPanelCriteria.setBorder(javax.swing.BorderFactory.createTitledBorder(
				javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 1),
				"SPECTROSCOPIC CRITERIA"/* + cataSelected */, javax.swing.border.TitledBorder.LEADING,
				javax.swing.border.TitledBorder.TOP, new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11),
				new java.awt.Color(60, 60, 60)));
		if (cataSelected.equals("Lovas 1992") || cataSelected.equals("Lovas 2003")
				|| cataSelected.equals("Big  Lovas")) {
			jPanelCriteria.add(createJPanelCriteriaDefault(cataSelected));

			//20220113 poniendo mire en lista
		}else if( Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB,cataSelected.toString().replace(" ", ""))>=0) {
//		} else if (cataSelected.toString().replace(" ", "").equals(SlimConstants.CATALOG_JPL) ||
//
//				cataSelected.toString().replace(" ", "").equals(SlimConstants.CATALOG_CDMS) ||
//
//				cataSelected.toString().replace(" ", "").equals(SlimConstants.CATALOG_USER)) {
			jPanelCriteria.add(createJPanelCriteriaJPLCDMS(cataSelected));
		} else if (cataSelected.toString().toUpperCase().contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {

			jPanelCriteria.add(createJPanelCriteriaRECOMB(cataSelected));
		}
	}

	/**
	 * Changes the Scale of Frenquecy/Wavelength in the Range Selection SubPanel
	 * 
	 * @param convTable Conversion table used to convert units
	 * @param from      Position in the array of the original units
	 * @param to        Position in the array of the new units
	 * @return double[] Converted units
	 *         TBD--------------------------------------------------- This should
	 *         receive as input parameters the values to be converted.
	 */
	public double[] unitConversionMinMax(double[] valueMinMax,double[] convTable, int from, int to) {
		double[] convertedUnits = valueMinMax;

		convertedUnits[0] = convertedUnits[0] * convTable[from] / convTable[to];
		convertedUnits[1] = convertedUnits[1] * convTable[from] / convTable[to];
		// System.out.println("From " + from + "to" + to + "---" +ConvertedUnits[0] + "
		// " + ConvertedUnits[1]);
		return convertedUnits;

	}

	public double unitConversionResolutionOld(String  resolution, double[] convTable, int from, int to) {
		if (!resolution.equals("")) {
			try {
				double convertedUnits = Double.parseDouble(resolution);
				convertedUnits = convertedUnits * convTable[from] / convTable[to];
				// System.out.println("From " + from + "to" + to + "---" +ConvertedUnits[0] + "
				// " + ConvertedUnits[1]);
				return convertedUnits;
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return Double.NaN;
	}


	/**
	 * Changes from Frequency to Wavelength and viceversa
	 * 
	 * @param to Indicates to which scale the units will be changed
	 * @return double[] Converted units. Frequency in MHz and Wavelength in mm
	 *         TBD--------------------------------------------------- This should
	 *         receive as input parameters the values to be converted.
	 */
	public double[] unitConversionMinMax(double[] valueMinMax, String to,JComboBox cBoxHzmm) 
	{
		double[] convertedUnits = new double[] { 0, 0 };
		if (to.equals("Frequency")) {
			convertedUnits = unitConversionMinMax(valueMinMax,arrayMmFactor, cBoxHzmm.getSelectedIndex(), WAVE_CM_POS_COMBO);
			convertedUnits[0] = 2.9979247E+10 / convertedUnits[0] / 1E+6;
			convertedUnits[1] = 2.9979247E+10 / convertedUnits[1] / 1E+6;
			//DEVUELTE EL VALOR EN FREQUENCIA, PERO EN LA QUE VOY A MOSTRAR EN EL COMBo, ESTO ES 
			//EN MHz
		} else if (to.equals("Wavelength")) {
			convertedUnits = unitConversionMinMax(valueMinMax,arrayHzFactor, cBoxHzmm.getSelectedIndex(), FREQ_HZ_POS_COMBO);

			convertedUnits[0] = 2.9979247E+10 / convertedUnits[0] * 10;
			convertedUnits[1] = 2.9979247E+10 / convertedUnits[1] * 10;

		}
		return convertedUnits;
	}
	
	public double unitConversionLineWidth(String resolution, String to) {
		double convertedUnits = Double.NaN;
		try {
			convertedUnits = new Double(resolution);
		}catch (NumberFormatException e) {
			// TODO: handle exception
		}

		if (to.equals("km/s")) {
		//	convertedUnits = unitConversionResolution(resolution, cBoxHzmmGenerateSpectra.getSelectedIndex());
			 
			convertedUnits = convertedUnits / 1E+3;
			// System.out.println(ConvertedUnits[0] + " " + ConvertedUnits[1]);
		} else if (to.equals("m/s")) {
			
			convertedUnits = convertedUnits* 1000;
		}
		return convertedUnits;

	}
	public double unitConversionResolutionOld(String resolution, String to) {
		double convertedUnits = 0;

		if (to.equals("Frequency")) {
			convertedUnits = unitConversionResolutionOld(resolution, arrayMmFactor, cBoxHzmmGenerateSpectra.getSelectedIndex(),
					WAVE_CM_POS_COMBO);
			convertedUnits = 2.9979247E+10 / convertedUnits / 1E+6;
			// System.out.println(ConvertedUnits[0] + " " + ConvertedUnits[1]);
		} else if (to.equals("Wavelength")) {
			convertedUnits = unitConversionResolutionOld(resolution, arrayHzFactor, cBoxHzmmGenerateSpectra.getSelectedIndex(),
					FREQ_HZ_POS_COMBO);
			convertedUnits = 2.9979247E+10 / convertedUnits * 10;
		}
		return convertedUnits;

	}
	
	/**
	 * Gets the frequency ranges covered by the selected spectra.
	 * 
	 * @return String with range
	 */
	public String getStringFrequencyRangeSelected() throws Exception {
		String stringRange = new String();
		String unit = "";
		String label = "";
		String returnArgRange = "";
		// if (!checkBoxFreq.isSelected())
		if (selectTypeRange.getSelectedIndex() == RANGE_USER_DEFINED) // USED DEFINED
		{
			stringRange = tfFreqMin.getText() + MyConstants.SEPARATOR_NIVEL_1 + tfFreqMax.getText()
					+ MyConstants.SEPARATOR_NIVEL_1;

			unit = cBoxHzmm.getSelectedItem() + "";
			if (unit.equals("\u03BCm"))
				unit = "micrometer";
			else if (unit.equals("\u00C5"))
				unit = "angstrom";
			label = cBoxNuLambda.getSelectedItem() + "";
			rangeSearch = null;
			returnArgRange = " range='" + stringRange + "' axislabel='" + label + "' axisunit='" + unit + "'";
		} else {
			if (!isMADCUBA_IJconnected()) {
				if (rangeSearch != null && !rangeSearch.equals("")) {// GET RANGE OPEN PRODUCT
					stringRange = rangeSearch;
					unit = "MHz";
					label = "Frequency";
					returnArgRange = " range='" + stringRange + "' axislabel='" + label + "' axisunit='" + unit + "'";

				} else
					JOptionPane.showMessageDialog(this, "IT IS NOT OPENED: A MASSAJ TABLE OR SPECTRAL");
			} else {

				if (selectTypeRange.getSelectedIndex() == RANGE_WINDOW) {
					stringRange = SELECTED_WINDOW;
				} else if (selectTypeRange.getSelectedIndex() == RANGE_WHOLE_FREQ) {
					stringRange = SELECTED_WHOLE_RANGE;
				} else {

					stringRange = SELECTED_DATA;
				}
				returnArgRange = " range='" + stringRange + "'";
			}
		}
		return returnArgRange;
	}

	/**
	 * Gets the frequency ranges covered by the selected spectra.
	 * 
	 * @return String with range
	 */
	public String getStringFrequencyRangeGenerateSpectra() {
		String stringRange = new String();
		String unit = "";
		String label = "";

		stringRange = tfFreqMinGenerate.getText() + MyConstants.SEPARATOR_NIVEL_1 + tfFreqMaxGenerate.getText()
				+ MyConstants.SEPARATOR_NIVEL_1;

		unit = cBoxHzmmGenerateSpectra.getSelectedItem() + "";
		if (unit.equals("\u03BCm"))
			unit = "micrometer";
		else if (unit.equals("\u00C5"))
			unit = "angstrom";
		label = cBoxNuLambdaGenerateSpectra.getSelectedItem() + "";
		rangeSearch = null;

		return " range='" + stringRange + "' axislabel='" + label + "' axisunit='" + unit + "'";
	}

	private String generateStringMoleculesCommand(String tablename) {
		// TODO CUANDO VAYA A ANADIR LAS MOLECULAS SI DE DIGO ADD; NO BUSQUE LAS QUE
		// EXISTAN
		String commandMolecules = new String();
		

		if (jpListMolecules.getTableSpecies().getRowCount() > 0) {
			if (jpListMolecules.getTableSpecies().getRowCount() > 4
					&& jpListMolecules.getTableSpecies().getRowCount() == jpListMolecules.getListModelIn().size())

			{
				if (!MyUtilities.showMessageWithNoYesOption(this,
						"Selected ALL molecules for searching?.\n Do you want to continue?",
						"WARNING: Search All MOlecules"))
					return null;
			}
			String sForm = null;
			String sCatalog = null;
			for (int irow = 0; irow < jpListMolecules.getTableSpecies().getRowCount(); irow++) {
				// System.out.println("S: ---" + jtTableSpecies.getValueAt(irow, 0) + "--- C:
				// ---" + jtTableSpecies.getValueAt(irow, 1) + "---");
				sForm = jpListMolecules.getTableSpecies().getValueAt(irow, 0) + "";
				sCatalog = jpListMolecules.getTableSpecies().getValueAt(irow, 1).toString();
				String command = generateCommandSearchCriteria(irow, sCatalog);
				commandMolecules += sCatalog + MyConstants.SEPARATOR_NIVEL_2 + sForm + MyConstants.SEPARATOR_NIVEL_2
						+ command + MyConstants.SEPARATOR_NIVEL_1;
			}
		} else {
			String commandCriteria = generateCommandSearchCriteria(false);
			if (jpListMolecules.getListIn().getSelectedIndex() >= 0) {
				for (int iSel = 0; iSel < jpListMolecules.getListIn().getSelectedValues().length; iSel++) {
					commandMolecules += tablename + MyConstants.SEPARATOR_NIVEL_2
							+ jpListMolecules.getListIn().getSelectedValues()[iSel] + MyConstants.SEPARATOR_NIVEL_2
							+ commandCriteria + MyConstants.SEPARATOR_NIVEL_1;
				}
			} else if (!jpListMolecules.getTextMolecules().getText().equals("")) {
				commandMolecules += tablename + MyConstants.SEPARATOR_NIVEL_2
						+ jpListMolecules.getTextMolecules().getText() + MyConstants.SEPARATOR_NIVEL_2 + commandCriteria
						+ MyConstants.SEPARATOR_NIVEL_1;
			}
		}		return commandMolecules;
	}

	private String generateCommandSearchCriteria(boolean isAllCriteria) {
		String commandCriteria = "";
		/*
		 * IF THERE ARE ENERGY CONSTRAINTS
		 */
		/*
		 * IF THERE ARE INTENSITY CONSTRAINTS
		 */
	

		if (jPanelCriteriaJPLCDMS!=null&& jPanelCriteriaJPLCDMS.getParent()!=null&& jPanelCriteriaJPLCDMS.isVisible()) {
			if (isAllCriteria)
				commandCriteria += "1" + MyConstants.SEPARATOR_NIVEL_2;
			if (tfEnergy.getText().equals(""))
				commandCriteria += "Any" + MyConstants.SEPARATOR_NIVEL_2;
			else
				commandCriteria += tfEnergy.getText() + MyConstants.SEPARATOR_NIVEL_2;

			if (tfEnergy2.getText().equals(""))
				commandCriteria += "Any" + MyConstants.SEPARATOR_NIVEL_2;
			else
				commandCriteria += tfEnergy2.getText() + MyConstants.SEPARATOR_NIVEL_2;

			if (tfIntensity.getText().equals(""))
				commandCriteria += "Any" + MyConstants.SEPARATOR_NIVEL_2;
			else
				commandCriteria += tfIntensity.getText() + MyConstants.SEPARATOR_NIVEL_2;

		} else if (jPanelCriteriaRECOMB!=null&&jPanelCriteriaRECOMB.getParent()!=null &&jPanelCriteriaRECOMB.isVisible()) {
			// commandCriteria += tfDeltaN.getText() + MyConstants.SEPARATOR_NIVEL_2;
			try {
				if (Integer.parseInt(tfDeltaN.getText()) < 1 || Integer.parseInt(tfDeltaN.getText()) > 12) {
					if (Integer.parseInt(tfDeltaN.getText()) < 1) {
						JOptionPane.showMessageDialog(this,
								MyConstants.LABEL_DELTA_NO_MAC + " has to be larger than 1. \n Value set to 2.",
								"Search Error", JOptionPane.ERROR_MESSAGE);
						tfDeltaN.setText("2");
					} else {
						JOptionPane.showMessageDialog(this,
								MyConstants.LABEL_DELTA_NO_MAC + " is currently limited to 12. \n Value set to 12.",
								"Search Error", JOptionPane.ERROR_MESSAGE);
						tfDeltaN.setText("12");
					}
					commandCriteria = tfDeltaN.getText();
				} else if (!tfDeltaN.getText().equals("")) {
					commandCriteria = tfDeltaN.getText();
				}
			} catch (NumberFormatException e) {
				tfDeltaN.setText("2");
				commandCriteria = tfDeltaN.getText();
			}
		}
		return commandCriteria;
	}

	private String generateCommandSearchCriteria(int irow, String catalog) {
		/*
		 * IF THERE ARE ENERGY CONSTRAINTS
		 */
		String commandCriteria = "";
		int iRowModel = jpListMolecules.getTableSpecies().convertRowIndexToModel(irow);

		if (catalog.toUpperCase().startsWith(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
			if (jpListMolecules.getTableSpecies().getModel().getValueAt(iRowModel,
					SlimConstants.POS_COLUMN_TMOLEC_DELTA_N) != null)
				commandCriteria = jpListMolecules.getTableSpecies().getModel()
						.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_DELTA_N).toString();
			else
				commandCriteria = "1";
		} else {
			String energy = "Any";
			if (jpListMolecules.getTableSpecies().getModel().getValueAt(iRowModel,
					SlimConstants.POS_COLUMN_TMOLEC_ELOW) != null
					&& !jpListMolecules.getTableSpecies().getModel()
							.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_ELOW).toString().equals(""))
				energy = jpListMolecules.getTableSpecies().getModel()
						.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_ELOW).toString();
			String energy2 = "Any";
			if (jpListMolecules.getTableSpecies().getModel().getValueAt(iRowModel,
					SlimConstants.POS_COLUMN_TMOLEC_CM_1) != null
					&& !jpListMolecules.getTableSpecies().getModel()
							.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_CM_1).toString().equals(""))
				energy2 = jpListMolecules.getTableSpecies().getModel()
						.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_CM_1).toString();

			commandCriteria += energy + MyConstants.SEPARATOR_NIVEL_2 + energy2 + MyConstants.SEPARATOR_NIVEL_2;

			/*
			 * IF THERE ARE INTENSITY CONSTRAINTS
			 */
			String intensity = "Any";
			if (jpListMolecules.getTableSpecies().getModel().getValueAt(iRowModel,
					SlimConstants.POS_COLUMN_TMOLEC_LOG) != null
					&& !jpListMolecules.getTableSpecies().getModel()
							.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_LOG).toString().equals(""))
				intensity = jpListMolecules.getTableSpecies().getModel()
						.getValueAt(iRowModel, SlimConstants.POS_COLUMN_TMOLEC_LOG).toString();

			commandCriteria += intensity + MyConstants.SEPARATOR_NIVEL_2;
		}
		return commandCriteria;
	}

	/**
	 * Sends the search query to the DB according to the selected criteria
	 * * 
	 * @param isGenerateSpectra-- indicated it is generate a new spectrum or only make search
	 */
	public void doSearch(boolean isGenerateSpectra) {
		/*
		 * TODO ESTO DEBERIA IR EN EL PLUGIN PARAMS A PASAR CATALOGO parametros que use
		 * para genererar sql command
		 */

		String argsPlugin = "";
		try {
			// TODO THIS SHOULD NOT BE NEEDED. DB SHOULD BE MODIFIED SO NO DIFFERENT NAMES
			// ARE NEEDED
			/*
			 * First gets the name of the table in DB corresponding to the Long Name
			 * selected in the Catalog Combo Box
			 */
			String comboCatalog = jpListMolecules.getComboCatalog().getSelectedItem().toString();
			/*
			 * String sqlcommand = "SELECT id_tablename FROM TableIndex "+
			 * "WHERE id_tablelongname = '".concat(comboCatalog).concat("'");
			 * 
			 * ResultSet dbresult = db.query(sqlcommand); dbresult.next(); comboCatalog =
			 * dbresult.getObject(1).toString();
			 */
//				argsPlugin = "catalog='"+comboCatalog+"'";

			// RANGE FREQUENCY / WAQVELENGTH
			if (isGenerateSpectra)
				argsPlugin += getStringFrequencyRangeGenerateSpectra();
			else {
				try {
					argsPlugin += getStringFrequencyRangeSelected();
				} catch (Exception e) {
					IJ.showMessage(e.getMessage());
					return;
				}
			}

			// IS NEW SEARCH OR EXITS SEARCH
			if (jpListMolecules.getRadioAddCurrent().isSelected() && !isGenerateSpectra)
				argsPlugin += " searchtype=add";
			else if (jpListMolecules.getRadioUpdate().isSelected() && !isGenerateSpectra)
				argsPlugin += " searchtype=update_transitions";

			String molecules = generateStringMoleculesCommand(comboCatalog);
			/*
			 * if (molecules == null) { dbresult.close(); System.gc(); return; }
			 */

			if (!molecules.equals("")) {
				argsPlugin += " molecules='" + molecules + "'";
			} else {
				argsPlugin += " criteria='" + generateCommandSearchCriteria(true) + "'";
			}
			/* dbresult.close(); */
			System.gc();
		} catch (Exception sqlException) {
		}

	//	 Log.getInstance().logger.debug("getNameFileMADCUBA()[0]="+getNameFileMADCUBA()[0]);
		if (!isGenerateSpectra && getNameFileMADCUBA() != null && selectTypeRange.getSelectedIndex() <= 1) {
			if (getNameFileMADCUBA()[0] != null && !getNameFileMADCUBA()[0].equals("")
					&& !getNameFileMADCUBA()[0].equals("null"))
				argsPlugin += " datafile='" + getNameFileMADCUBA()[0] + "'";
			if (getNameFileMADCUBA()[1] != null && !getNameFileMADCUBA()[1].equals("")
					&& !getNameFileMADCUBA()[1].equals("null"))
				argsPlugin += " datatype='" + getNameFileMADCUBA()[1] + "'";
		}

		if (isGenerateSpectra) {
//			if (tfResolutionMin.isEditable() && !tfResolutionMin.getText().equals("") && !tfResolutionMax.getText().equals(""))
//			{
//				argsPlugin += " resolution='" + tfResolutionMin.getText()+MyConstants.SEPARATOR_NIVEL_1+  tfResolutionMax.getText()+ "'";
//				argsPlugin += " resol_unit='"+cBoxVelocityGenerateSpectra.getSelectedItem()+"'";
//			//	argsPlugin += " resolution='400"+MyConstants.SEPARATOR_NIVEL_1+"600'";
//				
//			}
			if (tfLineWidth.isEditable() && !tfLineWidth.getText().equals(""))
			{
				argsPlugin += " linewidth='" + tfLineWidth.getText()+"'";
				argsPlugin += " linewidth_unit='"+cBoxVelocityGenerateSpectra.getSelectedItem()+"'";
			//	argsPlugin += " resolution='400"+MyConstants.SEPARATOR_NIVEL_1+"600'";
				
			}
			if (tfBeamSize.isEditable() && !tfBeamSize.getText().equals(""))
				argsPlugin += " beamsize='" + tfBeamSize.getText() + "'";
			if (tfBoxNoise.isEditable() && !tfBoxNoise.getText().equals(""))
				argsPlugin += " noise='" + tfBoxNoise.getText() + "'";
			argsPlugin += " unit_int='" + cBoxUnitsIntesity.getSelectedItem() + "'";
			argsPlugin += " tempscal_int='" + cBoxTempscalIntensity.getSelectedItem() + "'";
		}

		
		// CALL PLUGIN
		// Log.getInstance().logger.debug("b CALL PLUGIN ARG ="+argsPlugin);
		if (isGenerateSpectra)
			IJ.getInstance().runUserPlugIn(ListPluginsNamesSlim.PLUGIN_GENERATE_SLIM, ListPluginsNamesSlim.PLUGIN_GENERATE_SLIM,
					argsPlugin, false);
		else
			IJ.getInstance().runUserPlugIn(ListPluginsNamesSlim.PLUGIN_SEARCH_SLIM, ListPluginsNamesSlim.PLUGIN_SEARCH_SLIM,
					argsPlugin, false);
		IJ.runPlugIn(ListPluginsNamesSlim.SLIM_LOAD_TRANSITIONS, "load");
		
	}
	
	protected void popupErrorMessage(String errorMessage) {

		JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);

	}

	/**
	 * Initializes the User Interface
	 */
	public void initGUI() {
		/*
		 * Set the Layout of the JPanel containing the different Panels and includes
		 * them Sets the dimensions of the JPanel
		 *
		 */
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		jpListMolecules = new PanelListMolecules(this);
		// add(jPanelUpperButtons);
		add(jpListMolecules);
		add(jPanelFrequency);
		add(jPanelCriteria);
		add(createJPanelCollisionCatalog());
		add(createJPanelParametersSource());
		add(jPanelGenerateSpectra);

		Dimension preferredSize = new Dimension(panelWidth, 800);
		setSize(preferredSize);
		setPreferredSize(preferredSize);
		setMinimumSize(preferredSize);
		/*
		 * INDOCATED GENERIC FORMET TEXT
		 */

		format = new DecimalFormat("##0.##E0", dfs);

		
		/*
		 * JPanelCatalog JPanelFrequency and JPanelCriteria Layout Borders and Title
		 */

		// JPanelFrequency Range Criteria
		jPanelFrequency.setLayout(new BoxLayout(jPanelFrequency, BoxLayout.Y_AXIS));
		jPanelFrequency.setBorder(javax.swing.BorderFactory.createTitledBorder(
				javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 1), "SEARCH CRITERIA",
				javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
				new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11), new java.awt.Color(60, 60, 60)));
		jPanelFrequency.setPreferredSize(new java.awt.Dimension(panelWidth, 10));
		jPanelFrequency.setMinimumSize(new java.awt.Dimension(panelWidth, 5));

		
		// JPanel Spectroscopic Criteria
		jPanelCriteria.setBorder(javax.swing.BorderFactory.createTitledBorder(
				javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 1),
				"SPECTROSCOPIC CRITERIA", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
				new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11), new java.awt.Color(60, 60, 60)));
		jPanelCriteria.setLayout(new BoxLayout(jPanelCriteria, BoxLayout.Y_AXIS));
		jPanelCriteria.setPreferredSize(new java.awt.Dimension(panelWidth, 10));
		jPanelCriteria.setMinimumSize(new java.awt.Dimension(panelWidth, 5));
		// JPanel Generate Spectra
		jPanelGenerateSpectra.setBorder(javax.swing.BorderFactory.createTitledBorder(
				javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 1), "GENERATE SPECTRA",
				javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
				new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11), new java.awt.Color(60, 60, 60)));

		jPanelGenerateSpectra.setLayout(new BoxLayout(jPanelGenerateSpectra, BoxLayout.Y_AXIS));
		jPanelGenerateSpectra.setPreferredSize(new java.awt.Dimension(panelWidth, 10));
		jPanelGenerateSpectra.setMinimumSize(new java.awt.Dimension(panelWidth, 5));
		
		fillcBoxCatalog("ALL"); // The All option has no sense anymore as no other can be selected with radio
								// buttons.

		/*
		 * Fills Frequency Panel
		 */
		jpRangeCriteria.add(new JLabel("Range: "));
		selectTypeRange.addItem("Selected Spectra");
		selectTypeRange.addItem("Selected Window");
		selectTypeRange.addItem("User Defined");
		selectTypeRange.addItem("Whole");
		jpRangeCriteria.add(selectTypeRange);
		// jpFreqLine1.add(checkBoxWindow);
		/*
		 * When checkBoxFreq is selected the textfields are not editable if it is
		 * unselected, the textfields are editable
		 */
		ItemListener pclFreqAuto = new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (selectTypeRange.getSelectedIndex() != RANGE_USER_DEFINED/* checkBoxFreq.isSelected() */) {
					/*
					 * tfBoxNoise.setEditable(false); tfFreqMin.setEditable(false);
					 * tfFreqMax.setEditable(false); tfResolution.setEditable(false);
					 * tfBeamSize.setEditable(false);
					 */
					// checkBoxWindow.setEnabled(true);
					visibledPanelRange(false);
				} else {
					visibledPanelRange(true);
					/*
					 * tfFreqMin.setEditable(true); tfFreqMax.setEditable(true);
					 * tfResolution.setEditable(true); tfBeamSize.setEditable(true);
					 */
					// checkBoxWindow.setEnabled(false);
				}
			}

		};
		// checkBoxFreq.addItemListener(pclFreqAuto);
		selectTypeRange.addItemListener(pclFreqAuto);
		// cBoxNuLambda.removeAll();

		cBoxNuLambda.addItem("Frequency");
		cBoxNuLambda.addItem("Wavelength");
		jpRangeCriteria.add(cBoxNuLambda);
	//	 jpFreqLine1.add(lVeloSpectral); VER QUE HAGO CON ESTO

		tfFreqMin.setText("80000");
		tfFreqMin.setEditable(true);
		tfFreqMin.setCaretPosition(0);
		// tfFreqMin.addKeyListener(this);
		tfFreqMax.setText("300000");
		tfFreqMax.setEditable(true);
		// tfFreqMax.addKeyListener(this);

		/*
		 * GENERATE SPECTRA PANEL
		 */
		cBoxNuLambdaGenerateSpectra.addItem("Frequency");
		cBoxNuLambdaGenerateSpectra.addItem("Wavelength");
		tfFreqMinGenerate.setText("80000");
		tfFreqMinGenerate.setCaretPosition(0);
		tfFreqMinGenerate.addKeyListener(this);
		tfFreqMaxGenerate.setText("300000");
		tfFreqMaxGenerate.addKeyListener(this);
//		cBoxVelocityGenerateSpectra.setSelectedIndex(1);
//		tfResolutionMin.setEditable(true);
//		tfResolutionMax.setEditable(true);

		tfLineWidth.setEditable(true);
//		tfRestFrequency.setEditable(false);
		tfBeamSize.setText("10");
		tfBeamSize.setEditable(true);

		/*
		 * JPanel Generate Spectra
		 */
		JButton bSyntheticSpectra = new JButton("Synthetic Spectra");
		bSyntheticSpectra.setActionCommand("SyntheticSpectra");
		// bSyntheticSpectra.setEnabled(false);
		JButton bGenerateSpectra = new JButton("Generate");
		bGenerateSpectra.setActionCommand("GenerateSpectra");
		ActionListener alGenerateSpectra = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (ev.getActionCommand() != null && ev.getActionCommand().equals("SyntheticSpectra")) {
					panelGenerateSpectraVisible(!jpGenerateSpectraCriteria1.isVisible());

				} else if (ev.getActionCommand() != null && ev.getActionCommand().equals("GenerateSpectra")) {

					boolean isGenerate = false;
					/*
					 * 20161122 FIRTS VALIDATE IF IT EXISTS A DATA (CUBE OR SPECTRA) ELSE NO MAKE
					 * SEARCH
					 */
					boolean isStartCatalogNoRecomb = false;
					for (String catalog : SlimConstants.CATALOG_LIST_NO_RECOMB)
					{
						if(jpListMolecules.getComboCatalog().getSelectedItem().toString()
								.startsWith(catalog))
						{
							isStartCatalogNoRecomb=true;
							break;
						}
					}

					if (/*jpListMolecules.getComboCatalog().getSelectedItem().toString()
							.startsWith(SlimConstants.CATALOG_JPL)
							|| jpListMolecules.getComboCatalog().getSelectedItem().toString()
									.startsWith(SlimConstants.CATALOG_CDMS)
							|| jpListMolecules.getComboCatalog().getSelectedItem().toString()
									.startsWith(SlimConstants.CATALOG_USER)*/
							isStartCatalogNoRecomb
							|| jpListMolecules.getComboCatalog().getSelectedItem().toString()
									.startsWith(SlimConstants.CATALOG_RECOMBINE)) {
//						if ((tfResolutionMin.getText() == null || tfResolutionMin.getText().trim().equals(""))) {
//							IJ.showMessage("Resolution Minimum is empty it can't create spectral.");
//						} else if ((tfResolutionMax.getText() == null || tfResolutionMax.getText().trim().equals(""))) {
//							IJ.showMessage("Resolution Maximum is empty it can't create spectral.");
						if ((tfLineWidth.getText() == null || tfLineWidth.getText().trim().equals(""))) {
								IJ.showMessage("Line Width is empty it can't create spectral.");
						} else {
							isGenerate = true;
						}

						if (isGenerate) {
							/*
							 * if(jpListMolecules.getComboCatalog().getSelectedItem().toString().startsWith(
							 * SlimConstants.CATALOG_JPL) ||
							 * jpListMolecules.getComboCatalog().getSelectedItem().toString().startsWith(
							 * SlimConstants.CATALOG_CDMS) || CDMSHFS
							 * jpListMolecules.getComboCatalog().getSelectedItem().toString().startsWith(
							 * SlimConstants.CATALOG_USER)) {
							 */
							doSearch(true);
							/*
							 * }
							 * 
							 * else
							 * if(jpListMolecules.getComboCatalog().getSelectedItem().toString().startsWith(
							 * SlimConstants.CATALOG_RECOMBINE)) {
							 * IJ.showMessage("It is not implement code"); // doSearchRecombLines(true); }
							 */
						}
					}
				}
			}
		};
		bSyntheticSpectra.addActionListener(alGenerateSpectra);
		bGenerateSpectra.addActionListener(alGenerateSpectra);

		// cBoxHzmm.removeAll();
		cBoxHzmm.setName(COMBO_HZMM_CRITERIA);

		PopupMenuListener pmlNuLambda = new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
				if (cBoxNuLambdaSelected != cBoxNuLambda.getSelectedIndex()) {
					eventCBoxNuLambda();
					// cBoxNuLambdaGenerateSpectra.setSelectedIndex(cBoxNuLambdaSelected);
					// eventCBoxNuLambdaGenerateSpectra();

				}
			}

			public void popupMenuCanceled(PopupMenuEvent ev) {
			}
		};
		cBoxNuLambda.addPopupMenuListener(pmlNuLambda);

		cBoxHzmmGenerateSpectra.setName(COMBO_HZMM_GENERATE);
		PopupMenuListener pmlNuLambdaGenerate = new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
				if (cBoxNuLambdaGenerateSelected != cBoxNuLambdaGenerateSpectra.getSelectedIndex()) {
					eventCBoxNuLambdaGenerateSpectra();
					// cBoxNuLambda.setSelectedIndex(cBoxNuLambdaGenerateSelected);
					// eventCBoxNuLambda();
				}
			}

			public void popupMenuCanceled(PopupMenuEvent ev) {
			}
		};
		
		cBoxNuLambdaGenerateSpectra.addPopupMenuListener(pmlNuLambdaGenerate);

		PopupMenuListener pmlHzmm = new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
				if (isMakeEventChangeHzmm) {
					isMakeEventChangeHzmm = false;
					eventCBoxHzmm();
					// cBoxHzmmGenerateSpectra.setSelectedItem(cBoxHzmm.getSelectedItem());
					// eventCBoxHzmmGenerateSpectra();
				}
				isMakeEventChangeHzmm = true;
				;

				// tfFreqMax.setCaretPosition(0);
				// tfFreqMin.setCaretPosition(0);
				// tfResolution.setCaretPosition(0);
				// tfRestFrequency.setCaretPosition(0);

				cBoxHzmmSelected = cBoxHzmm.getSelectedIndex();
			}

			public void popupMenuCanceled(PopupMenuEvent ev) {
			}
		};
		cBoxHzmm.addPopupMenuListener(pmlHzmm);

		PopupMenuListener pmlHzmmGenerate = new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
	
				if (isMakeEventChangeHzmmGenerate) {
					isMakeEventChangeHzmmGenerate = false;
					eventCBoxHzmmGenerateSpectra();

					// cBoxHzmm.setSelectedItem(cBoxHzmmGenerateSpectra.getSelectedItem());
					// eventCBoxHzmm();
				}
				isMakeEventChangeHzmmGenerate = true;

				// tfFreqMax.setCaretPosition(0);
				// tfFreqMin.setCaretPosition(0);
				// tfResolution.setCaretPosition(0);
				// tfRestFrequency.setCaretPosition(0);
				cBoxHzmmGenerateSelected = cBoxHzmmGenerateSpectra.getSelectedIndex();
			}

			public void popupMenuCanceled(PopupMenuEvent ev) {
			}
		};
		cBoxHzmmGenerateSpectra.addPopupMenuListener(pmlHzmmGenerate);

		cBoxNuLambda.setSelectedIndex(0);
		cBoxNuLambdaSelected = 0;
		fillcBoxHzmm(arrayHz, 2, cBoxHzmm);
		jpRangeCriteria.add(cBoxHzmm);
		jpRangeCriteria.add(lFreqMin);
		jpRangeCriteria.add(tfFreqMin);
		jpRangeCriteria.add(lFreqMax);
		jpRangeCriteria.add(tfFreqMax);

		// jpGenerateSpectraCriteria2.setLayout(new
		// BoxLayout(jpGenerateSpectraCriteria2, BoxLayout.X_AXIS));

		cBoxNuLambdaGenerateSpectra.setSelectedIndex(0);
		cBoxNuLambdaGenerateSelected = 0;
		fillcBoxHzmm(arrayHz, 2, cBoxHzmmGenerateSpectra);
		jpGenerateSpectraCriteria1.add(new JLabel("Noise(\u03C3)"));
		jpGenerateSpectraCriteria1.add(tfBoxNoise);
		jpGenerateSpectraCriteria1.add(cBoxUnitsIntesity);
		cBoxUnitsIntesity.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED)
				{
					if(cBoxUnitsIntesity.getSelectedItem().equals("Jy"))
					{
						cBoxTempscalIntensity.setSelectedIndex(0);
						cBoxTempscalIntensity.setEnabled(false);
					} else {

						cBoxTempscalIntensity.setEnabled(true);
					}
				}
				
			}
		});
		
		jpGenerateSpectraCriteria1.add(cBoxTempscalIntensity);
		jpGenerateSpectraCriteria1.add(lBeamSize);
		jpGenerateSpectraCriteria1.add(tfBeamSize);
		jpGenerateSpectraCriteria2.add(new JLabel("Range"));
		jpGenerateSpectraCriteria2.add(cBoxNuLambdaGenerateSpectra);
		jpGenerateSpectraCriteria2.add(cBoxHzmmGenerateSpectra);
		jpGenerateSpectraCriteria2.add(new JLabel("From"));
		jpGenerateSpectraCriteria2.add(tfFreqMinGenerate);
		jpGenerateSpectraCriteria2.add(new JLabel("To"));
		jpGenerateSpectraCriteria2.add(tfFreqMaxGenerate);
		jpGenerateSpectraCriteria3.add(lResolution);
		cBoxVelocityGenerateSpectra.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED)
				{
//					if(cBoxVelocityGenerateSpectra.getSelectedItem().equals("Km/s"))
//					{
						double lineWidth=unitConversionLineWidth(tfLineWidth.getText(),cBoxVelocityGenerateSpectra.getSelectedItem()+"");
					
						if(!Double.isNaN(lineWidth))
						{
							tfLineWidth.setText(formatNumericToString(lineWidth));
						}
//					} else {
//
//						unitConversionResolutionNew(reso)
//						unitConversionResolutionNew(reso);
//					}
				}
				
			}
		});
		
		cBoxVelocityGenerateSpectra.setSelectedIndex(1);
		jpGenerateSpectraCriteria3.add(cBoxVelocityGenerateSpectra);
////		jpGenerateSpectraCriteria3.add(new JLabel("From"));
//		jpGenerateSpectraCriteria3.add(tfResolutionMin);
//		jpGenerateSpectraCriteria3.add(new JLabel("To"));
//		jpGenerateSpectraCriteria3.add(tfResolutionMax);
		jpGenerateSpectraCriteria3.add(tfLineWidth);
		tfLineWidth.addKeyListener(this);
//		tfResolutionMin.addKeyListener(this);
//		tfResolutionMax.addKeyListener(this);
//20220321 dE MOMENTO QUITADO PORQUE VAMOS A CAMBIAR LA FILOSOFIA, SOLO LO QUITO DE 
//DE LA VISUALIZACION, PROQUE NO SE SI AFECTA A LA GENERACION
//		jpGenerateSpectraCriteria2.add(lRestFrequency);
//		jpGenerateSpectraCriteria2.add(tfRestFrequency);
		jpGenerateSpectraCriteria3.add(bGenerateSpectra);

		// jpGenerateSpectraCriteria1.add(new JLabel("''"));

		jPanelFrequency.add(jpRangeCriteria);
		// jPanelFrequency.add(jpFreqLine2);
		JPanel panelButoonSynthetic = new JPanel();
		// panelButoonGenerate.setMinimumSize(new java.awt.Dimension(panelWidth/4, 20));
		// panelButoonGenerate.setPreferredSize(new java.awt.Dimension(panelWidth, 20));
		jPanelGenerateSpectra.setMinimumSize(new java.awt.Dimension(panelWidth + 180, 20));
		jPanelGenerateSpectra.setPreferredSize(new java.awt.Dimension(panelWidth, 20));

		// panelButoonGenerate.setLayout(new BoxLayout(panelButoonGenerate,
		// BoxLayout.X_AXIS));
		// panelButoonGenerate.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panelButoonSynthetic.add(bSyntheticSpectra);
		panelButoonSynthetic.add(jpGenerateSpectraCriteria1);
		jPanelGenerateSpectra.add(panelButoonSynthetic);
		jPanelGenerateSpectra.add(jpGenerateSpectraCriteria2);
		jPanelGenerateSpectra.add(jpGenerateSpectraCriteria3);
		// jpGenerateSpectraCriteria2.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panelGenerateSpectraVisible(false);

		/*
		 * TESTS if the database is accesible
		 */
		try {
			jpListMolecules.getComboCatalog().setSelectedIndex(0);
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			_log.logger.warn("check that you have a 'lines.data' database in your installation");
		} catch (Exception e) {
			// e.printStackTrace();
			_log.logger.warn("check that you have a 'lines.data' database in your installation");
		}
		// checkBoxFreq.setSelected(false);
		selectTypeRange.setSelectedIndex(RANGE_USER_DEFINED);
	}

	private void eventCBoxNuLambda() {
		if (cBoxNuLambdaSelected != cBoxNuLambda.getSelectedIndex()) {
			double[] convertedUnits = new double[] { Double.parseDouble(tfFreqMin.getText()),
					Double.parseDouble(tfFreqMax.getText()) };
			double[] auxFreq = unitConversionMinMax(convertedUnits,cBoxNuLambda.getSelectedItem().toString(),cBoxHzmm);
			tfFreqMin.setText(formatNumericToString(auxFreq[0]));
			tfFreqMax.setText(formatNumericToString(auxFreq[1]));
			if (cBoxNuLambda.getSelectedItem().equals("Frequency")) {
				fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmm);
			} else if (cBoxNuLambda.getSelectedItem().equals("Wavelength")) {
				fillcBoxHzmm(arrayMm, WAVE_DEFAULT_COMBO, cBoxHzmm);
			}
			cBoxNuLambdaSelected = cBoxNuLambda.getSelectedIndex();
		}
	}

	private void eventCBoxNuLambdaGenerateSpectra() {
		if (cBoxNuLambdaGenerateSelected != cBoxNuLambdaGenerateSpectra.getSelectedIndex()) {

			double[] auxFreqIni = new double[] { Double.parseDouble(tfFreqMinGenerate.getText()),
					Double.parseDouble(tfFreqMaxGenerate.getText()) };
			
			double[] auxFreq = unitConversionMinMax(auxFreqIni,cBoxNuLambdaGenerateSpectra.getSelectedItem().toString(),cBoxHzmmGenerateSpectra);
	//		double resolution = unitConversionResolution(tfResolution.getText(),cBoxNuLambdaGenerateSpectra.getSelectedItem().toString());
	//		double restFreq = unitConversionRestFreq(tfRestFrequency.getText(),cBoxNuLambdaGenerateSpectra.getSelectedItem().toString());
			tfFreqMinGenerate.setText(formatNumericToString(auxFreq[0]));
			tfFreqMaxGenerate.setText(formatNumericToString(auxFreq[1]));

//			if (!Double.isNaN(resolution))
//				tfResolution.setText(formatNumericToString(resolution));
//			if (!Double.isNaN(restFreq))
//				tfRestFrequency.setText(formatNumericToString(restFreq));
			if (cBoxNuLambdaGenerateSpectra.getSelectedItem().equals("Frequency")) {
				fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmmGenerateSpectra);
			} else if (cBoxNuLambdaGenerateSpectra.getSelectedItem().equals("Wavelength")) {
				fillcBoxHzmm(arrayMm, WAVE_DEFAULT_COMBO, cBoxHzmmGenerateSpectra);
			}

			cBoxNuLambdaGenerateSelected = cBoxNuLambdaGenerateSpectra.getSelectedIndex();
		}
	}

	private void eventCBoxHzmm() {
		double[] auxFreq = new double[] { Double.parseDouble(tfFreqMin.getText()),
				Double.parseDouble(tfFreqMax.getText()) };
		if (cBoxNuLambda.getSelectedItem().equals("Frequency")) {
			auxFreq = unitConversionMinMax(auxFreq,arrayHzFactor, cBoxHzmmSelected, cBoxHzmm.getSelectedIndex());
			tfFreqMin.setText(formatNumericToString(auxFreq[0]));
			tfFreqMax.setText(formatNumericToString(auxFreq[1]));
		} else if (cBoxNuLambda.getSelectedItem().equals("Wavelength")) {
			auxFreq = unitConversionMinMax(auxFreq,arrayMmFactor, cBoxHzmmSelected, cBoxHzmm.getSelectedIndex());
			tfFreqMin.setText(formatNumericToString(auxFreq[0]));
			tfFreqMax.setText(formatNumericToString(auxFreq[1]));
		}
	}

	private void eventCBoxHzmmGenerateSpectra() {
		double[] auxFreq;
		auxFreq = new double[] { Double.parseDouble(tfFreqMinGenerate.getText()),
				Double.parseDouble(tfFreqMaxGenerate.getText()) };
	
		if (cBoxNuLambdaGenerateSpectra.getSelectedItem().equals("Frequency")) {

			auxFreq = unitConversionMinMax(auxFreq,arrayHzFactor, cBoxHzmmGenerateSelected,
					cBoxHzmmGenerateSpectra.getSelectedIndex());		
				
//			resolution = unitConversionResolution(tfResolution.getText(),arrayHzFactor, cBoxHzmmGenerateSelected,
//					cBoxHzmmGenerateSpectra.getSelectedIndex());
//			restFreq = unitConversionRestFreq(tfRestFrequency.getText(),arrayHzFactor, cBoxHzmmGenerateSelected,
//					cBoxHzmmGenerateSpectra.getSelectedIndex());
		
			tfFreqMinGenerate.setText(formatNumericToString(auxFreq[0]));
			tfFreqMaxGenerate.setText(formatNumericToString(auxFreq[1]));
//			if (!Double.isNaN(resolution)) {
//				tfResolution.setText(formatNumericToString(resolution));
//			}
//			if (!Double.isNaN(restFreq)) {
//				tfRestFrequency.setText(formatNumericToString(restFreq));
//			}
		} else if (cBoxNuLambdaGenerateSpectra.getSelectedItem().equals("Wavelength")) {
			auxFreq = unitConversionMinMax(auxFreq,arrayMmFactor, cBoxHzmmGenerateSelected,
					cBoxHzmmGenerateSpectra.getSelectedIndex());
//			resolution = unitConversionResolution(tfResolution.getText(),arrayMmFactor, cBoxHzmmGenerateSelected,
//					cBoxHzmmGenerateSpectra.getSelectedIndex());
//			restFreq = unitConversionRestFreq(tfRestFrequency.getText(),arrayMmFactor, cBoxHzmmGenerateSelected,
//					cBoxHzmmGenerateSpectra.getSelectedIndex());
			tfFreqMinGenerate.setText(formatNumericToString(auxFreq[0]));
			tfFreqMaxGenerate.setText(formatNumericToString(auxFreq[1]));
//			if (!Double.isNaN(resolution))
//				tfResolution.setText(formatNumericToString(resolution));
//			if (!Double.isNaN(restFreq))
//				tfRestFrequency.setText(formatNumericToString(restFreq));
		}
	}

	private void visibledPanelRange(boolean isVisible) {
		tfFreqMin.setVisible(isVisible);
		tfFreqMax.setVisible(isVisible);
		cBoxNuLambda.setVisible(isVisible);
		cBoxHzmm.setVisible(isVisible);

		lFreqMin.setVisible(isVisible);
		lFreqMax.setVisible(isVisible);
	}

	private void panelGenerateSpectraVisible(boolean isVisible) {

		jpGenerateSpectraCriteria1.setVisible(isVisible);
		jpGenerateSpectraCriteria2.setVisible(isVisible);
		jpGenerateSpectraCriteria3.setVisible(isVisible);
	}
	private void panelSourceParametersVisible(boolean isVisible) {

		jPanelParametersSource.setVisible(isVisible);
	}
	public void clearSearch() {
		jpListMolecules.setNameOpenFile(null);
		jpListMolecules.deselectAll();
		jpListMolecules.getTextMolecules().setText("");
		jpListMolecules.autoFilter();
		jpListMolecules.clearListOut(true);
		if (getTfEnergy() != null) {
			getTfEnergy().setText("Any");
			getTfEnergy2().setText("Any");
			getTfIntensity().setText("Any");
		}
		if (getTfDeltaN() != null)
			getTfDeltaN().setText("2");
		if (getcBoxNuLambda() != null) {
			getcBoxNuLambda().setSelectedIndex(0);
			fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmm);
		}
		if (getTfFreqMin() != null)
			getTfFreqMin().setText("80000");
		if (getTfFreqMax() != null)
			getTfFreqMax().setText("300000");

		// PANEL GENRATE SPECTRA
		if (getcBoxNuLambdaGenerateSpectra() != null) {
			getcBoxNuLambdaGenerateSpectra().setSelectedIndex(0);
			fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmmGenerateSpectra);
		}

		if (getTfFreqMinGenerate() != null)
			getTfFreqMinGenerate().setText("80000");
		if (getTfFreqMaxGenerate() != null)
			getTfFreqMaxGenerate().setText("300000");

		if(cBoxVelocityGenerateSpectra!=null&&cBoxVelocityGenerateSpectra.getItemCount()>1)
			cBoxVelocityGenerateSpectra.setSelectedIndex(1);

		if (getTfLineWidth() != null)
			getTfLineWidth().setText("");
		
//		if (getTfResolutionMin() != null)
//			getTfResolutionMin().setText("");
//		if (getTfResolutionMax() != null)
//			getTfResolutionMax().setText("");
//
//		if (getTfRestFrequency() != null)
//			getTfRestFrequency().setText("");
//		getTfRestFrequency().setEditable(false);

		if (getTfBeamSize() != null)
			getTfBeamSize().setText("10");
		if (getTfNoise() != null)
			getTfNoise().setText("");

		// checkBoxFreq.setSelected(false);
		selectTypeRange.setSelectedIndex(RANGE_USER_DEFINED);
		rangeSearch = null;
		// nameFileMADCUBA = new String[2];
	}

	public void resetGUI() {
		jpListMolecules.setNameOpenFile(null);
		jpListMolecules.deselectAll();
		jpListMolecules.getTextMolecules().setText("");
		jpListMolecules.autoFilter();
		jpListMolecules.clearListOut(true);
		jpListMolecules.getRadioAddCurrent().setSelected(true);

		if (getTfEnergy() != null) {
			getTfEnergy().setText("Any");
			getTfEnergy2().setText("Any");
			getTfIntensity().setText("Any");
		}
		if (getTfDeltaN() != null)
			getTfDeltaN().setText("2");
		if (getcBoxNuLambda() != null) {
			getcBoxNuLambda().setSelectedIndex(0);
			cBoxNuLambdaSelected = 0;
			fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmm);
		}
		if (getTfFreqMin() != null)
			getTfFreqMin().setText("80000");
		if (getTfFreqMax() != null)
			getTfFreqMax().setText("300000");

		// PANEL GENRATE SPECTRA
		if (getcBoxNuLambdaGenerateSpectra() != null) {
			getcBoxNuLambdaGenerateSpectra().setSelectedIndex(0);
			cBoxNuLambdaGenerateSelected = 0;
			fillcBoxHzmm(arrayHz, FREQ_DEFAULT_COMBO, cBoxHzmmGenerateSpectra);
		}

		if (getTfFreqMinGenerate() != null)
			getTfFreqMinGenerate().setText("80000");
		if (getTfFreqMaxGenerate() != null)
			getTfFreqMaxGenerate().setText("300000");
		if(cBoxVelocityGenerateSpectra!=null&&cBoxVelocityGenerateSpectra.getItemCount()>1)
			cBoxVelocityGenerateSpectra.setSelectedIndex(1);

		if (getTfLineWidth() != null)
			getTfLineWidth().setText("");
//		if (getTfResolutionMin() != null)
//			getTfResolutionMin().setText("");
//		if (getTfResolutionMax() != null)
//			getTfResolutionMax().setText("");

//		if (getTfRestFrequency() != null)
//			getTfRestFrequency().setText("");
//		getTfRestFrequency().setEditable(false);

		if (getTfBeamSize() != null)
			getTfBeamSize().setText("10");
		if (getTfNoise() != null)
			getTfNoise().setText("");

		try {
			jpListMolecules.getComboCatalog().setSelectedIndex(0);
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			// _log.warn("check that you have a 'lines.data' database in your
			// installation");
		} catch (Exception e) {
			// e.printStackTrace();
			// _log.warn("check that you have a 'lines.data' database in your
			// installation");
		}
		// checkBoxFreq.setSelected(false);
		selectTypeRange.setSelectedIndex(RANGE_USER_DEFINED);

		rangeSearch = null;
		nameFileMADCUBA = new String[2];

		panelGenerateSpectraVisible(false);
		
		jPanelCollisionMolecule.resetPanel();
		jPanelParametersSource.resetPanel();
		panelSourceParametersVisible(false);

	}

	public void initialsParams() {
		for (int iC = 0; iC < jpListMolecules.getComboCatalog().getItemCount(); iC++) {
			if (jpListMolecules.getComboCatalog().getItemAt(iC).toString().contains(SlimConstants.CATALOG_JPL)) {
				jpListMolecules.getComboCatalog().setSelectedIndex(iC);
				break;
			}
		}
		if (isMADCUBA_IJconnected()) {

			selectTypeRange.setSelectedIndex(0);
			// checkBoxFreq.setEnabled(true);
			// checkBoxFreq.setSelected(true);
	//		lVeloSpectral.setText(getVelocityFromMADCUBA_IJ());
		}
	}

	/*
	 * private void setSizeButtonImage(JButton button) { button.setMinimumSize(new
	 * Dimension(100, 35)); button.setMaximumSize(new Dimension(145, 50));
	 * button.setPreferredSize(new Dimension(100, 35)); button.setSize(145,35); }
	 */
	public String formatNumericToString(double value) {
		String sFormat = format.format(value);
		if (sFormat.endsWith("E0"))
			sFormat = sFormat.substring(0, sFormat.indexOf("E0"));
		return sFormat;
	}

	
	public boolean isMADCUBA_IJconnected() {
		return AstronomicalFunctionsGenerics.isMADCUBA_IJconnected();
	}
	
	public String[] getNameMADCUBA_IJconnected() {
		String dataName[] = new String[3];
		dataName[2] = null;
		// 0 name file cube or tab massaj
		// 1 type file (CUBE or SPECTRA)
		if (!isMADCUBA_IJconnected()) {
			return null;
		} else {
			//primero ver si existe syncro
			if(AstronomicalFunctionsGenerics.isSynchronizeCubesconnected())
			{
				dataName[0] = SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW;//TENDRE que tener en algun sitio la lista de cubos
				dataName[1] = SlimConstants.FITS_TYPE_CUBE_SYNCHRO;
			} else if (AstronomicalFunctionsGenerics.isAstronomical(WindowManager.getCurrentImage())) {
				dataName[0] = WindowManager.getCurrentImage().getTitle().replaceFirst(AstronomicalImagePlus.PREFIX_CUBE,
						"");
				dataName[1] = SlimConstants.FITS_TYPE_CUBE;
				//LO CONVIERTO EN SYNCHRO
				SynchronizeCubePlugin frameSynchro = (SynchronizeCubePlugin)WindowManager.getFrame(SynchronizeCubePlugin.SYNCHRONIZE_CUBES_TITLE); 
		    	boolean isSynchronizeOpen =  frameSynchro!=null  && frameSynchro.isVisible();
		    	
				if(isSynchronizeOpen && AstronomicalFunctionsGenerics.isSynchronizeCubesconnected())
				{
				
			    		frameSynchro.removeAllSelectCube();							
				} else {
					 IJ.getInstance().runUserPlugIn(ListPluginsNames.PLUGIN_OPEN_SYNCHRONIZE, ListPluginsNames.PLUGIN_OPEN_SYNCHRONIZE, null, false);
		    		 frameSynchro = (SynchronizeCubePlugin)WindowManager.getFrame(SynchronizeCubePlugin.SYNCHRONIZE_CUBES_TITLE); 
			   		 isSynchronizeOpen =  frameSynchro!=null  && frameSynchro.isVisible();
			   		 
				}
	    		IJ.wait(10);
	    		if(isSynchronizeOpen)
	    		{
		    		frameSynchro.addSelections(WindowManager.getCurrentImage().getTitle(),true);
		    		dataName[0] = SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW;//TENDRE que tener en algun sitio la lista de cubos
					dataName[1] = SlimConstants.FITS_TYPE_CUBE_SYNCHRO;
	    		}
			} else if (AstronomicalFunctionsGenerics.isAstronomicalPlotWindow(WindowManager.getCurrentImage())) {
				dataName[0] = WindowManager.getCurrentImage().getTitle()
						.replaceFirst(AstronomicalImagePlus.PREFIX_CUBE, "")
						.replaceFirst(AstronomicalImagePlus.PREFIX_PLOT, "");
				int typePlot = ((AstronomicalPlotWindow) WindowManager.getCurrentImage().getWindow()).getTypePlot();
				if (typePlot == AstronomicalPlotWindow.PLOT_CUBE)
				{
					
					dataName[1] = SlimConstants.FITS_TYPE_CUBE;
					SynchronizeCubePlugin frameSynchro = (SynchronizeCubePlugin)WindowManager.getFrame(SynchronizeCubePlugin.SYNCHRONIZE_CUBES_TITLE); 
			    	boolean isSynchronizeOpen =  frameSynchro!=null  && frameSynchro.isVisible();
			    	
					if(isSynchronizeOpen && AstronomicalFunctionsGenerics.isSynchronizeCubesconnected())
					{
					
				    		frameSynchro.removeAllSelectCube();							
					} else {
						 IJ.getInstance().runUserPlugIn(ListPluginsNames.PLUGIN_OPEN_SYNCHRONIZE, ListPluginsNames.PLUGIN_OPEN_SYNCHRONIZE, null, false);
			    		 frameSynchro = (SynchronizeCubePlugin)WindowManager.getFrame(SynchronizeCubePlugin.SYNCHRONIZE_CUBES_TITLE); 
				   		 isSynchronizeOpen =  frameSynchro!=null  && frameSynchro.isVisible();
				   		 
					}
		    		IJ.wait(10);
		    		if(isSynchronizeOpen)
		    		{
		    			if(AstronomicalFunctionsGenerics.isAstronomicalPlotFromBackground(WindowManager.getCurrentImage()))
		    				frameSynchro.addSelections("#"+WindowManager.getCurrentImage().getTitle()+"#",true);
		    			else
		    				frameSynchro.addSelections(WindowManager.getCurrentImage().getTitle().replaceFirst(AstronomicalImagePlus.PREFIX_PLOT,AstronomicalImagePlus.PREFIX_CUBE  ),true);
			    		dataName[0] = SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW;//TENDRE que tener en algun sitio la lista de cubos
						dataName[1] = SlimConstants.FITS_TYPE_CUBE_SYNCHRO;
		    		}
				}
				else if (typePlot == AstronomicalPlotWindow.PLOT_MASSAJ)
					dataName[1] = SlimConstants.FITS_TYPE_SPECTRAL;

			} else if (WindowManager.getFrame(AstronomicalJFrameMASSA.FRAME_MASSAJ) != null
					&& WindowManager.getFrame(AstronomicalJFrameMASSA.FRAME_MASSAJ).isVisible()) {
				AstronomicalJTableMASSA massajTable = AstronomicalFunctionsGenerics.getCurrentAstronomicalJTableMASSA();
				if(massajTable!=null)
				{
					dataName[0] = massajTable.getPanelManager().getNameTabbedParent() + MyConstants.SEPARATOR_TABS
							+ massajTable.getNameTab();
					dataName[1] = SlimConstants.FITS_TYPE_SPECTRAL;
				}
			}
		}
		return dataName;
	}

	private String getVelocityFromMADCUBA_IJ() {
		String valueVelo = null;
		double dVelocity = AstronomicalFunctionsGenerics.getVelocityFromMADCUBA_IJ(null, true);

		if (!Double.isNaN(dVelocity)) {
			int digits = MyUtilities.getDigitsDecimal(dVelocity);
			Unit unit = AstronomicalChangeUnit.getUnitDefault(MyConstants.X_AXIS_VRAD, digits);
			dVelocity = AstronomicalChangeUnit.changeUnit(Speed.METERS_PER_SECOND, unit, dVelocity);

			String sVelo = format.format(dVelocity);
//			Log.getInstance().logger.debug(dVelocity+"---velo--"+sVelo);
			if (sVelo.endsWith("E0"))
				sVelo = sVelo.substring(0, sVelo.indexOf("E0"));

			valueVelo = "Velocity Spectral: " + sVelo + " " + unit.getDialogName();
		}

		return valueVelo;
	}

	public PanelListMolecules getPanelCatalog() {
		return jpListMolecules;
	}

	public JTextField getTfEnergy() {
		return tfEnergy;
	}

	public JTextField getTfEnergy2() {
		return tfEnergy2;
	}

	public JTextField getTfIntensity() {
		return tfIntensity;
	}

	public JTextField getTfDeltaN() {
		return tfDeltaN;
	}

	/*
	 * public static JCheckBox getCheckBoxFreq() { return checkBoxFreq; }
	 */
	public static JComboBox getSelectTypeRange() {
		return selectTypeRange;
	}

	public static JTextField getTfFreqMin() {
		return tfFreqMin;
	}

	public static JTextField getTfFreqMax() {
		return tfFreqMax;
	}

	public static JTextField getTfFreqMinGenerate() {
		return tfFreqMinGenerate;
	}

	public static JTextField getTfFreqMaxGenerate() {
		return tfFreqMaxGenerate;
	}

//	public JTextField getTfResolutionMin() {
//		return tfResolutionMin;
//	}
//
//	public JTextField getTfResolutionMax() {
//		return tfResolutionMax;
//	}


	public JTextField getTfLineWidth() {
		return tfLineWidth;
	}
	public JTextField getTfNoise() {
		return tfBoxNoise;
	}

//	public JTextField getTfRestFrequency() {
//		return tfRestFrequency;
//	}

	public JTextField getTfBeamSize() {
		return tfBeamSize;
	}

	public JComboBox getcBoxHzmm() {
		return cBoxHzmm;
	}

	public int getcBoxHzmmSelected1() {
		return cBoxHzmmSelected;
	}

	public void setcBoxHzmmSelected(int cBoxHzmmSelected) {
		this.cBoxHzmmSelected = cBoxHzmmSelected;
	}

	public static JComboBox getcBoxNuLambda() {
		return cBoxNuLambda;
	}

	public int getcBoxNuLambdaSelected() {
		return cBoxNuLambdaSelected;
	}

	public void setcBoxNuLambdaSelected(int cBoxNuLambdaSelected) {
		this.cBoxNuLambdaSelected = cBoxNuLambdaSelected;
	}

	public JComboBox getcBoxHzmmGenerateSpectra() {
		return cBoxHzmmGenerateSpectra;
	}

	public int getcBoxHzmmGenerateSelected() {
		return cBoxHzmmGenerateSelected;
	}

	public void setcBoxHzmmGenerateSelected(int cBoxHzmmGenerateSelected) {
		this.cBoxHzmmGenerateSelected = cBoxHzmmGenerateSelected;
	}

	public static JComboBox getcBoxNuLambdaGenerateSpectra() {
		return cBoxNuLambdaGenerateSpectra;
	}

	public int getcBoxNuLambdaGenerateSelected() {
		return cBoxNuLambdaGenerateSelected;
	}

	public void setcBoxNuLambdaGenerateSelected(int cBoxNuLambdaGenerateSelected) {
		this.cBoxNuLambdaGenerateSelected = cBoxNuLambdaGenerateSelected;
	}

	public void setRangeSearch(String rangeSearch) {
		this.rangeSearch = rangeSearch;
	}

	public String getRangeSearch() {
		return rangeSearch;
	}

	public void setNameFileMADCUBA(String[] nameFileMADCUBA, boolean isEventMenu, boolean isChangeSimParams) {
		if (isEventMenu) {
			if (!changeDataSearchCriteria2(nameFileMADCUBA))
				return;
		}

		this.nameFileMADCUBA = nameFileMADCUBA;

		if(nameFileMADCUBA!=null &&	nameFileMADCUBA.length>1 &&nameFileMADCUBA[0]!=null&&
				!nameFileMADCUBA[0].isEmpty()&& !nameFileMADCUBA[0].equals(SlimConstants.NAME_FITS_TYPE_CUBE_SYNCHRO_NEW))
		{
			SLIMFRAME.getInstance(false).enabledMenusParamsCubes(false);

		} else {

			SLIMFRAME.getInstance(false).enabledMenusParamsCubes(true);
		}
	}

	public String[] getNameFileMADCUBA() {
		return nameFileMADCUBA;
	}

	public static DbHSQLDBCreate getDB() {
		return db;
	}

	public LineSearchResultsDialog getDialogResults() {
		return dialogResults;
	}

	public String getSelectedMoleculeQNLTE()
	{
		return jPanelCollisionMolecule.getCatalogCollision();
	}
	public void setSelectedMoleculeQNLTE(String catalogCollision)
	{
		jPanelCollisionMolecule.setCatalogCollision(catalogCollision);
	}

//	public void setVelocityRadial(String value)
//	{
//		jPanelQNLTE.setVelocityRad(value);
//	}
//	public void setVelocityRadial(double value)
//	{
//		jPanelQNLTE.setVelocityRad(value);
//	}
//	public String getVelocityRadial()
//	{
//		return jPanelQNLTE.getVelocityRad();
//	}
//	public void setLineWidth(String value)
//	{
//		jPanelQNLTE.setLineWidth(value);
//	}
//	public void setLineWidth(double value)
//	{
//		jPanelQNLTE.setLineWidth(value);
//	}
//	public String getLineWidth()
//	{
//		return jPanelQNLTE.getLineWidth();
//	}
//	public void setTkSource(String value)
//	{
//		jPanelQNLTE.setTk_Source(value);
//	}
//	public void setTkSource(double value)
//	{
//		jPanelQNLTE.setTk_Source(value);
//	}
//	public String getTkSource()
//	{
//		return jPanelQNLTE.getTk_Source();
//	}
//
//
//	public String getLogNH2()
//	{
//		return jPanelQNLTE.getLogNH2();
//	}
//
//	public void setLogNH2(double value)
//	{
//		jPanelQNLTE.setLogNH2(value);
//	}
//	public void setLogNH2(String value)
//	{
//		jPanelQNLTE.setLogNH2(value);
//	}
	public String[] getDataCatalogQ_nLTE()
	{
		return jPanelCollisionMolecule.getDataCatalogQ_nLTE();
	}

	public String[][] getDataSourceQ_nLTE()
	{
		return jPanelParametersSource.getDataSourceQ_nLTE();
	}

	public void updateSourceQNLTE() {
		
		if(SLIMFRAME.getInstance(false).getPanelModelLTESimFit()==null
			||SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter==null
			||SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter.tabSearchSIMFIT==null)
			 return;
		String nameFile = SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter.tabSearchSIMFIT.getReadDataTransitionsSelectedTab().getNameDirectoyTab();
		
		//Y vulvo a poner el source para guardar
		String [][] valuesCatalogQ_nLTE = getDataSourceQ_nLTE();
//		SlimUtilitiesFiles.saveSourceParameters(nameFile,
//				SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter.getPixelShow(),
//				valuesCatalogQ_nLTE);
		//GUARDAR SOLO EN EL SEARCH
		SlimUtilitiesFiles.saveSourceParametersSearch(nameFile,
				valuesCatalogQ_nLTE);
		if(valuesCatalogQ_nLTE!=null &&valuesCatalogQ_nLTE.length>=1)
			SlimUtilitiesFiles.savePropertiesParametersSource(valuesCatalogQ_nLTE[0]);
		

		
	}
	public double[] changeUnitFreqDefault(double[] valueMinMax) {
		return unitConversionMinMax(valueMinMax,arrayHzFactor, cBoxHzmmSelected, FREQ_MHZ);
	}

	public double[] changeUnitWaveDefault(double[] valueMinMax) {
		return unitConversionMinMax(valueMinMax,arrayMmFactor, cBoxHzmmSelected, WAVE_MM_DEFAULT);
	}


	public static boolean existMolecule(String catalog, String molecule) {
		 String sqlcommand = "SELECT distinct id_formula  FROM " + catalog + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
	  	  
		ResultSet dbresult = null;
		try {

			dbresult = db.query(sqlcommand);
			if(dbresult.next())
		            return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.getInstance().logger.error(catalog+"/"+molecule+" ERROR: "+e.getMessage());
			EditorInformationMADCUBA.append("WARNING:" +catalog+"/"+molecule+" NOT EXISTS IN BD: "+e.getMessage());
	//		e.printStackTrace();
		}finally {
			if(dbresult!=null)
			{
				try {
					dbresult.close();
				} catch (SQLException e) {
				}
			}
		}

        return false;
	}
	public static boolean existMolecule(String molecule) {
		if(molecule==null)
			return false;
		for (String catalog : SlimConstants.CATALOG_LIST_NO_RECOMB)
		{			
			String sqlcommand = "SELECT distinct id_formula  FROM " + catalog + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
			ResultSet dbresult = null;
			try {

				dbresult = db.query(sqlcommand);
				if(dbresult.next())
			            return true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}			
		}
		

       return false;
	}


	public static ArrayList<String> catalogsMolecule(String molecule) {
		ArrayList<String> listCatalog = new ArrayList<String>();
			
		for (String catalog : SlimConstants.CATALOG_LIST_NO_RECOMB)
		{	
			String sqlcommand = "SELECT distinct id_formula  FROM " + catalog + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
					listCatalog.add(catalog);
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}			
		}
		
		return listCatalog;
	}
	
//
//	public static ArrayList getQNsMolecule(String molecule, String catalog) {
//		String qnfmt = "";
//			
//		if(catalog!=null && Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, catalog.toUpperCase())>=0)
//		{ //MIRAR COMO HACER PARA USER
//			String sqlcommand = "SELECT distinct id_QNFMT  FROM " + catalog.toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
//		  	  
//			ResultSet dbresult = null;
//			try {
////				Log.getInstance().logger.debug(sqlcommand);
//				dbresult = db.query(sqlcommand);
//				if(dbresult!=null && dbresult.next())
//					qnfmt = dbresult.getObject(1)+"";
//			} catch (SQLException e) {
//			}finally {
//				if(dbresult!=null)
//				{
//					try {
//						dbresult.close();
//					} catch (SQLException e) {
//					}
//				}
//			}			
//		} else if(catalog!=null && catalog.contains("UNK")) {
//			ArrayList<String> catalogs  = catalogsMolecule( molecule);
//			if(catalogs.isEmpty())
//				return "";
//			String sqlcommand = "SELECT distinct id_QNFMT  FROM " + catalogs.get(0).toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
//		  	  
//			ResultSet dbresult = null;
//			try {
////				Log.getInstance().logger.debug(sqlcommand);
//				dbresult = db.query(sqlcommand);
//				if(dbresult!=null && dbresult.next())
//					qnfmt = dbresult.getObject(1)+"";
//			} catch (SQLException e) {
//			}finally {
//				if(dbresult!=null)
//				{
//					try {
//						dbresult.close();
//					} catch (SQLException e) {
//					}
//				}
//			}	
//			
//		}else if(catalog!=null && catalog.contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
//			return "1";
//		}
//		
//		return qnfmt;
//	}

	public static String[] getTagMolecule(String molecule, String catalog) {
		String[] tagCatalog = null;
		String tag = null;
		String catalogFinal = catalog;	
		if(catalog!=null && Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, catalog.toUpperCase())>=0)
		{ //MIRAR COMO HACER PARA USER
			String sqlcommand = "SELECT TOP 1 id_TAG  FROM " + catalog.toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
					tag = dbresult.getObject(1)+"";
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}			
		} else if(catalog!=null && catalog.contains("UNK")) {
			ArrayList<String> catalogs  = catalogsMolecule( molecule);
			if(catalogs.isEmpty())
				return null;
			String sqlcommand = "SELECT TOP 1 id_TAG  FROM " + catalogs.get(0).toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
				{
					tag = dbresult.getObject(1)+"";
					catalogFinal = catalogs.get(0);
				}
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}	
			
		}else if(catalog!=null && catalog.contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
			return null; //YA VEREMOS COMO HACER RECOMBINACION
		}
		
		if(tag!=null)
		{
			tagCatalog = new String[] {tag, catalogFinal};
		}
		return tagCatalog;
	}
	
	public static String getQNFMTMolecule(String molecule, String catalog) {
		String qnfmt = "";
			
		if(catalog!=null && Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, catalog.toUpperCase())>=0)
		{ //MIRAR COMO HACER PARA USER
			String sqlcommand = "SELECT TOP 1 id_QNFMT  FROM " + catalog.toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
					qnfmt = dbresult.getObject(1)+"";
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}			
		} else if(catalog!=null && catalog.contains("UNK")) {
			ArrayList<String> catalogs  = catalogsMolecule( molecule);
			if(catalogs.isEmpty())
				return "";
			String sqlcommand = "SELECT TOP 1 distinct id_QNFMT  FROM " + catalogs.get(0).toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
					qnfmt = dbresult.getObject(1)+"";
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}	
			
		}else if(catalog!=null && catalog.contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
			return "1";
		}
		
		return qnfmt;
	}
	public static float[] getPartFuncMolecule(String molecule, String catalog) {
		float[] partFunct = null;
			
		if(catalog!=null && Arrays.binarySearch(SlimConstants.CATALOG_LIST_NO_RECOMB, catalog.toUpperCase())>=0)
		{ //MIRAR COMO HACER PARA USER
			String sqlcommand = "SELECT TOP 1 id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9  FROM " + catalog.toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
				{
					partFunct = new float[7];
					for(int i=1; i<=7; i++)
					{
						if(dbresult.getObject(i)!=null)
						{
							try {
								partFunct [i-1]= new Float(dbresult.getObject(i)+"");
							} catch (Exception e) {
								partFunct [i-1]= Float.NaN;
							}
							
						} else {
							partFunct [i-1]= Float.NaN;							
						}
					}
				}
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}			
		} else if(catalog!=null && catalog.contains("UNK")) {
			ArrayList<String> catalogs  = catalogsMolecule( molecule);
			if(catalogs.isEmpty())
				return null;
			String sqlcommand = "SELECT TOP 1 id_QLOG300,id_QLOG225,id_QLOG150,id_QLOG75,id_QLOG37,id_QLOG18,id_QLOG9  FROM " + catalogs.get(0).toUpperCase() + "Cat WHERE UPPER(id_formula)='"+molecule.toUpperCase()+"'";
		  	  
			ResultSet dbresult = null;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				if(dbresult!=null && dbresult.next())
				{
					partFunct = new float[7];
					for(int i=1; i<=7; i++)
					{
						if(dbresult.getObject(i)!=null)
						{
							try {
								partFunct [i-1]= new Float(dbresult.getObject(i)+"");
							} catch (Exception e) {
								partFunct [i-1]= Float.NaN;
							}
							
						} else {
							partFunct [i-1]= Float.NaN;							
						}
					}
				}
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}	
			
		}else if(catalog!=null && catalog.contains(SlimConstants.CATALOG_RECOMBINE.toUpperCase())) {
			return partFunct; //HAY QUE VER COMO SE CALCULA AQUI
		}
		
		return partFunct;
	}
	
	
	
	
	
	
	public static ArrayList<Object[]> listMolecules() {
		String qnfmt = "";
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		for(String catalog : SlimConstants.CATALOG_LIST_NO_RECOMB)
		{ //MIRAR COMO HACER PARA USER
			String sqlcommand = "SELECT Distinct id_formula " +
					"FROM " + catalog + "Cat;" ;
		  	  
			ResultSet dbresult = null;
			Object[] row = new Object[19];
			row[0] = catalog;
			try {
//				Log.getInstance().logger.debug(sqlcommand);
				dbresult = db.query(sqlcommand);
				while(dbresult!=null && dbresult.next())
				{
				
					String sForm = dbresult.getObject(1)+"";

					boolean existsColumnQN7 = SLIMSearch.validateExistsQn7(catalog, db);
//					if(catalog.toUpperCase().equals(SlimConstants.CATALOG_USER))
//						
					if(!existsColumnQN7)
						sqlcommand = "SELECT Distinct id_formula,  id_TAG, id_QNFMT,id_DR,  "+
			      			"id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,' ' as id_QN7,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6,' ' as id_QNN7 " +
							"FROM " + catalog + "Cat, "+catalog+" WHERE "+catalog+".id_TAG="+catalog+"cat.id_TAG " +
							"AND id_formula='"+ sForm+"';" ;
					else
						 sqlcommand = "SELECT Distinct id_formula,  id_TAG, id_QNFMT,id_DR,  "+
					      			"id_QN1,id_QN2,id_QN3,id_QN4,id_QN5,id_QN6,id_QN7,id_QNN1,id_QNN2,id_QNN3,id_QNN4,id_QNN5,id_QNN6,id_QNN7 " +
									"FROM " + catalog + "Cat, "+catalog+" WHERE "+catalog+".id_TAG="+catalog+"cat.id_TAG " +
									"AND id_formula='"+ sForm+"';" ;

					ResultSet dbresult2 = null;
					dbresult2 = db.query(sqlcommand);
					try
					{
						if(dbresult2!=null && dbresult2.next())
						{
		
							row[1] = dbresult2.getObject(1)+"";//molecule
							row[2] = dbresult2.getObject(2)+"";//tag
							row[3] = dbresult2.getObject(3)+"";//qnfmt
							row[4] = dbresult2.getObject(4)+"";//dr
							row[5] = dbresult2.getObject(5)+"";//qn1
							row[6] = dbresult2.getObject(6)+"";//qn2
							row[7] = dbresult2.getObject(7)+"";//qn3
							row[8] = dbresult2.getObject(8)+"";//qn4
							row[9] = dbresult2.getObject(9)+"";//qn5
							row[10] = dbresult2.getObject(10)+"";//qn6

							row[11] = dbresult2.getObject(11)+"";//qn7
							row[12] = dbresult2.getObject(12)+"";//qnn1
							row[13] = dbresult2.getObject(13)+"";//qnn2
							row[14] = dbresult2.getObject(14)+"";//qnn3
							row[15] = dbresult2.getObject(15)+"";//qnn4
							row[16] = dbresult2.getObject(16)+"";//qnn5
							row[17] = dbresult2.getObject(17)+"";//qnn6
							row[18] = dbresult2.getObject(18)+"";//qnn7
							
//							row[18] = dbresult2.getObject(18)+"";//qnn7
/*							row[11] = dbresult2.getObject(11)+"";//qn7
							row[12] = dbresult2.getObject(12)+"";//qnn1
							row[13] = dbresult2.getObject(13)+"";//qnn2
							row[14] = dbresult2.getObject(14)+"";//qnn3
							row[15] = dbresult2.getObject(15)+"";//qnn4
							row[16] = dbresult2.getObject(16)+"";//qnn5
							row[17] = dbresult2.getObject(17)+"";//qnn6
//							row[18] = dbresult2.getObject(18)+"";//qnn7
 */
							list.add(row.clone());
						}
					}finally {
							if(dbresult2!=null)
							{
								try {
									dbresult2.close();
								} catch (SQLException e) {
								}
							}
					}
					
				}
			} catch (SQLException e) {
			}finally {
				if(dbresult!=null)
				{
					try {
						dbresult.close();
					} catch (SQLException e) {
					}
				}
			}			
		} 
		
		return list;
	}
	private boolean changeDataSearchCriteria2(String[] dataSelected) {

		if (dialogResults != null) {
			SLIMFRAME slimFrame = ((SLIMFRAME) WindowManager.getFrame(SlimConstants.FRAME_SLIM));// listSlectedMolecules();

			if (dataSelected != null) {
				if (slimFrame != null) {
					if (dataSelected != null && slimFrame.getPanelModelLTESimFit() != null) {

						slimFrame.getPanelModelLTESimFit().changeComboUnitIntensity(dataSelected);
					}
					// dataSelected = SlimUtilities.changeUnitIntensity(dataSelected);
//						if(dataSelected==null)
//						{
//							EditorInformationMADCUBA.appendLineError("The unit no exists. Indicated name in Modify_Data, Tab 'Operations'");
//							return false;
//						}
//					}
				}
//				Log.getInstance().logger.debug(dataSelected[0]+","+dataSelected[1]);
				// CAMBIO LOS DATOS DE LA BUSQUEDA DE CRITERIOS CON LO CUAL
				// EN EL LABEL DEBE CAMBIR EL PRODUCTO DE BASELINE
				dialogResults.setLabelFileFITS(dataSelected[0], dataSelected[1], true);
				dialogResults.setFilePath(null);//TENDRIA QUE PONER DIRECTORIO DEL FICEHRO SELECCIONADO. ¿DONDE LE TENGO?

			} else {
				// CAMBIO LOS DATOS DE LA BUSQUEDA DE CRITERIOS CON LO CUAL
				// EN EL LABEL DEBE CAMBIR EL PRODUCTO DE BASELINE
				dialogResults.setLabelFileFITS("", "", true);
				dialogResults.setFilePath(null);
			}

			SlimUtilities.closeAllPlotsSlim(true);
			// EMPTY SELECT SIMULATE
			if (slimFrame != null && slimFrame.getPanelModelLTESimFit() != null
					&& slimFrame.getPanelModelLTESimFit().sFitter != null) {// VACIAR TODO LO QUE NECESITO VACIAS
				// dialogResults.getTabResults().setNullListTransitionAllTab();
				slimFrame.getPanelModelLTESimFit().sFitter.closeCubeParams();
				slimFrame.getPanelModelLTESimFit().sFitter.setIndexListTransition(null);
				slimFrame.getPanelModelLTESimFit().sFitter.setNullListTransition();
				slimFrame.getPanelModelLTESimFit().sFitter.setValueNoise(Double.NaN);
				slimFrame.getPanelModelLTESimFit().sFitter.emptyDataTemp();
				// CREO QUE TODAVIA FALTA ALGO POR VACIAR //AVERIGUAR
				slimFrame.getPanelModelLTESimFit().sFitter.emptyDataProduct();// CREIQUE DEBERIA VACIARLO
//				slimFrame.getPanelModelLTESimFit().sFitter.connectToSpectra(dataSelected);
			}
			slimFrame.emptyDataUndo();
		}
		return true;
	}

	public void keyPressed(KeyEvent arg0) {

	}

	public void keyReleased(KeyEvent arg0) {
		// RECALCULATE VALUE REST FREQ AND SHOW
//		if (!tfResolution.getText().equals("")) {
//			try {
//				double resolution = Double.parseDouble(tfResolution.getText());
//				double freqFin = Double.parseDouble(tfFreqMax.getText());
//				double freqIni = Double.parseDouble(tfFreqMin.getText());
//
//				int numChannels = (int) (Math.abs(freqFin - freqIni) / resolution); // REDONDEAR
//				double crpix3 = numChannels / 2;
//				double dRestFreq = freqIni + crpix3 * resolution;
////				tfRestFrequency.setText(formatNumericToString(dRestFreq));
//			} catch (Exception e) {
////				tfRestFrequency.setText("");
//			}
//		} else {
////			tfRestFrequency.setText("");
//		}
//		tfRestFrequency.setCaretPosition(0);
	}

	public void keyTyped(KeyEvent arg0) {

	}

	public void fillParamsCriteriaFromGenerate(SearchSlimParams params) {
//	   IJ.showMessage("VOY A RELLENAR");

		LineSearchPanel.getSelectTypeRange().setSelectedIndex(RANGE_USER_DEFINED);
		String nameUnitPrincipal = params.getLabelAxis();
		String labelUnitSecondary = params.getLabelUnitAxis();
		if (labelUnitSecondary == null || labelUnitSecondary.equals("null"))
			labelUnitSecondary = getcBoxHzmm().getSelectedItem() + "";

		if (labelUnitSecondary.equals("micrometer"))
			labelUnitSecondary = "\u03BCm";
		else if (labelUnitSecondary.equals("angstrom"))
			labelUnitSecondary = "\u00C5";

		if (nameUnitPrincipal != null && !nameUnitPrincipal.equals("null")) {
			LineSearchPanel.getcBoxNuLambda().setSelectedItem(nameUnitPrincipal);
			if (LineSearchPanel.getcBoxNuLambda().getSelectedIndex() == 0) {
				fillcBoxHzmmFreqCriteriaDefault();
				setcBoxNuLambdaSelected(0);
			} else if (LineSearchPanel.getcBoxNuLambda().getSelectedIndex() == 1) {
				fillcBoxHzmmWaveCriteriaDefault();
				setcBoxNuLambdaSelected(1);
			}
		} else {
			nameUnitPrincipal = LineSearchPanel.getcBoxNuLambda().getSelectedItem() + "";
		}

		cBoxHzmm.setSelectedItem(labelUnitSecondary);
		cBoxHzmmSelected = cBoxHzmm.getSelectedIndex();

		// Hz
		double valueRangeIni = Double.NaN;
		double valueRangeFin = Double.NaN;

		if (params.getArrListRange() != null && params.getArrListRange().size() >= 2) {
			valueRangeIni = params.getArrListRange().get(0);
			valueRangeFin = params.getArrListRange().get(1);
		}

		if (!Double.isNaN(valueRangeIni) && !Double.isNaN(valueRangeFin)) {
			if (!Double.isNaN(valueRangeIni))
				LineSearchPanel.getTfFreqMin().setText(valueRangeIni + "");
			if (!Double.isNaN(valueRangeFin))
				LineSearchPanel.getTfFreqMax().setText(valueRangeFin + "");
			setRangeSearch(null);// DEBERIA PONER EL DE LA BUSQUEDA
		}
	}

	public void loadCustomizeCatalog() throws IOException {
		JFileChooser chooser;
		int returnVal;
		String lastPath = null;
		// AQUI IRA LA RUTA DE DEFECTO (GUARDAR EN UN PROPERTIES)

		if (PropertiesLastConfig.getInstance()
				.getProperty(PropertiesLastConfig.PROPERTY_SLIM_PATH_DATABASE_USER) != null)
			lastPath = PropertiesLastConfig.getInstance()
					.getProperty(PropertiesLastConfig.PROPERTY_SLIM_PATH_DATABASE_USER);
		else
			lastPath = "usercatalog/";

		if (lastPath == null || lastPath.equals("null")) {
			if (IJ.getVersion().compareTo("1.43u") >= 0) {
				lastPath = OpenDialog.getDefaultDirectory();
			} else {
				lastPath = "";
			}
		}
		chooser = new JFileChooser(lastPath);// new JFileChooser(_current_dir);
		chooser.addChoosableFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return "Directories containing catalog files from USER";
			}

			@Override
			public boolean accept(File f) {
				// TODO Auto-generated method stub
				return f.isDirectory();
			}
		});

		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		chooser.setDialogTitle("Select directory to open");
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		int lenghtChoose = chooser.getChoosableFileFilters().length;
		chooser.setFileFilter(chooser.getChoosableFileFilters()[lenghtChoose - 1]);
		chooser.setAcceptAllFileFilterUsed(false);
		returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File fi = chooser.getSelectedFile();
			if (fi != null) {
				if (!fi.exists()) {
					IJ.showMessage("Indicated directory not exists");
					return;
				}
				// defaultPath = fi.getParent();
				lastPath = fi.getAbsolutePath() + System.getProperty("file.separator");
			}

			// IJ.showMessage("DIRECTOYR:="+lastPath);
			/*
			 * try { db.shutdown(); } catch (SQLException e) { // TODO Auto-generated catch
			 * block e.printStackTrace(); }
			 */
			PropertiesLastConfig.getInstance().addProperty(PropertiesLastConfig.PROPERTY_SLIM_PATH_DATABASE_USER,
					lastPath);

			Prefs.savePreferences();

			EditorInformationMADCUBA.append("Create User Table: Loading ...\n");
			// EditorAllSpecies.getInstanceEditor().setAlwaysOnTop(true);
			try {
				//db = db.CreateUSERtableinDB(lastPath);
				db.CreateUSERdatabase(lastPath);
			} catch (Exception e) {
				e.printStackTrace();
				EditorInformationMADCUBA.appendLineError("Create User Table:" + e.getMessage());
			}
			EditorInformationMADCUBA.append("Create User Table: Terminate.\n");
			// EditorAllSpecies.getInstanceEditor().setAlwaysOnTop(false);
			fillcBoxCatalog("ALL");
		}
	}
	public void reloadUpdateCatalog(boolean isUserDB) throws IOException {
		if(db==null)
			return;

		
		EditorInformationMADCUBA.append("Create  Table: Loading ...\n");
		// EditorAllSpecies.getInstanceEditor().setAlwaysOnTop(true);
		try {
			//db = db.CreateUSERtableinDB(lastPath);
			db.reConnectNewDatabase(isUserDB);
		} catch (Exception e) {
			e.printStackTrace();
			EditorInformationMADCUBA.appendLineError("Create  Table:" + e.getMessage());
		}
		EditorInformationMADCUBA.append("Create  Table: Terminate.\n");
		IJ.wait(10);
		fillcBoxCatalog("ALL");
		IJ.wait(10);
		//createNewdatabase
	}

	public void updateComboQ_nLTE() {
		if(jPanelCollisionMolecule==null)
			jPanelCollisionMolecule.updateComboQ_nLTE();
	}

	public void setTkSource(String value) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setTk_Source(value,"1");
	}
	public void setTkSource(String value, String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setTk_Source(value,component);
	}
	public String getTkSource() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getTk_Source("1");
	}

	public String getTkSource(String component) {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getTk_Source(component);
	}
	public String getVelocityRadial() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getVelocityRadial("1");
	}

	public String getVelocityRadial(String component) {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getVelocityRadial(component);
	}
	public void setVelocityRadial(String value) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setVelocityRadial(value,"1");
	}
	public void setVelocityRadial(String value, String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setVelocityRadial(value,component);
	}
	public String getLineWidth() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getLineWidth("1");
	}

	public String getLineWidth(String component) {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getLineWidth(component);
	}
	public void setLineWidth(String value) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setLineWidth(value,"1");
	}
	public void setLineWidth(String value, String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setLineWidth(value,component);
	}
	public String getLogNH2() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getLogNH2("1");
	}

	public void setLogNH2(String value) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setLogNH2(value,"1");
	}
	public void setLogNH2(String value, String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setLogNH2(value,component);
	}
	
	public String getLogNH2(String component) {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getLogNH2(component);
	}

	public String getDeltaLogNH2() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getDeltaLogNH2("1");
	}

	public void setDeltaLogNH2(String value) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setDeltaLogNH2(value,"1");
	}
	public void setDeltaLogNH2(String value, String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setDeltaLogNH2(value,component);
	}
	
	public String getDeltaLogNH2(String component) {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getDeltaLogNH2(component);
	}
	

	public String getnNH2() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getnH2("1");
	}

	public void setnH2(String value) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setnH2(value,"1");
	}
	public void setnH2(String value, String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.setnH2(value,component);
	}
	
	public String getnH2(String component) {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getnH2(component);
	}
	
	public void setSourceParameters(String[][] data) {
		double dVeloIni = Double.NaN;
		if(SLIMFRAME.getInstance(false).getPanelModelLTESimFit()!=null
				&& SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter()!=null
				&& SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter().spectraConnector!=null)
			dVeloIni = SLIMFRAME.getInstance(false).getPanelModelLTESimFit().getSimFitter().spectraConnector.astroPlots.get(0).getSpectral().getSpectralWCS().getAltrVal() / 1000;
		
//		if(data==null)
//		{
////			setVelocityRadial("");
//			setLineWidth("");
//			setTkSource("");
//			setLogNH2("");	
//			return;
//		}
//		for(String[] value1: data)
//		{
//			Log.getInstance().logger.debug("data VALUES ROW");
//			for(String value:value1)
//				Log.getInstance().logger.debug(value);
//				
//		}
		if(jPanelParametersSource!=null)
			jPanelParametersSource.setSourceCatalogQnLTE( data,dVeloIni);
		
	}
	public void setVelocityRadialAll(double dVeloIni) {

		jPanelParametersSource.setVelocityRadialAll( dVeloIni);	
	}

	public void addComponent(String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.addParametersGest(component);
	}

	public void deleteComponent(String component) {
		// TODO Auto-generated method stub
		jPanelParametersSource.deleteParametersFin(component);
	}
	public int getCountComponents() {
		// TODO Auto-generated method stub
		return jPanelParametersSource.getCountComponents();
	}

	public void resetSourceParameters() {
		jPanelParametersSource.resetPanel();
		
	}
	public ArrayList<Object> getChangeComponentsSource() {
		return jPanelParametersSource.getChangeComponents();
		
	}
	public void clearChangeComponentsSource() {
		if(jPanelParametersSource.getChangeComponents()!=null)
			jPanelParametersSource.getChangeComponents().clear();
		
	}
	
	

	public void refreshParametersSourcePanel() {
		if(SLIMFRAME.getInstance(false).getPanelModelLTESimFit()!=null && SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter!=null
				 && SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter.tabSearchSIMFIT!=null
				 && SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter.tabSearchSIMFIT.getReadDataTransitions()!=null)
		{
				
			String[][] dataSource = SlimUtilitiesFiles.importSourceParametersSearch(SLIMFRAME.getInstance(false).getPanelModelLTESimFit().sFitter.tabSearchSIMFIT.getReadDataTransitions().getNameDirectoyTab());
//			Log.getInstance().logger.debug(dataSource);
			if(dataSource!=null)
			{

				setSourceParameters(dataSource);
			} 
			//SI NO HACEMOS LO E ARRIBA, PONER UN RESET()
			
		} 
	}
	
}