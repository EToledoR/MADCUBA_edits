package es.smr.slim.utils;


import es.cab.madcuba.utils.MyConstants;

public class SlimConstants 
{
	//VERSION PRODUCT
	public static final String SLIM_VERSION = "12";// PARA VER CUANDO TIENE QUE ACTUALIZAR LOS QMNTF
	public static final String SLIM_VERSION_TAB = "SLIMVERSION";
	public static final String MADCUBA_VERSION_TAB = "VERSION";
	
	//NAME FRAME 
	public static final String FRAME_SLIM = "SLIM";
	public static final String FRAME_SLIMFRAME = "SLIM_Window";
		
	//TYPE TABLES
	public static final int TABLE_DEFAULT = 0;
	public static final int TABLE_JPL_CDMS = 1;
	public static final int TABLE_LOVAS = 2;
	public static final int TABLE_RECOMBINE = 3;
	public static final int TABLE_BIGLOVAS = 4;
	public static final int TABLE_ALL = 5;
	
	//public BD

	public static final String NAME_FILES_BD = "lines";
	public static final String NAME_FILES_BD_USER = "linesuser";
	public static final String NAME_FILES_BD_Q_LTE = "linesqnlte";
	
	//public TYPE CATALOG STRING
	public static final String CATALOG_Q_NLTE = "QNLTE";
	public static final String CATALOG_USER = "USER";
	public static final String CATALOG_CDMS = "CDMS";
	/**
	 * Eduardo Toledo Sep 2024
	 * Changes to add the HITRAN catalog
	 */
	public static final String CATALOG_HITRAN = "HITRAN";
	/**
	 * Eduardo Toledo Dec 2024
	 * Changes to add the CDMS ortho para catalog
	 */
	public static final String CATALOG_CDMSOP = "CDMSOP";
	public static final String CATALOG_CDMSHFS = "CDMSHFS";
	public static final String CATALOG_JPL = "JPL";
	public static final String CATALOG_LOVAS = "Lovas";
	public static final String CATALOG_SMALLLOVAS = "Small";
	public static final String CATALOG_RECOMBINE = "Recomb";
	public static final String CATALOG_BIGLOVAS = "Big";
	public static final String CATALOG_LSD = "LSD";
	// Eduardo Toledo, Sergio Martin Dec 2024 This list needs to be sorted alphabetically
	public static final String[] CATALOG_LIST_NO_RECOMB = new String[] {CATALOG_CDMS,CATALOG_CDMSHFS,CATALOG_CDMSOP,CATALOG_HITRAN, CATALOG_JPL,CATALOG_LSD, CATALOG_USER}; 
//	public static final ArrayList<String> CATALOG_LIST_NO_RECOMB_ARRAY = new ArrayList<String>(new String[] {CATALOG_CDMS,CATALOG_CDMSHFS,CATALOG_JPL,CATALOG_USER}); 
	 
	//public static final String CATALOG_LOVAS03 = ;

	
	
	//CRITERIOS BUSQUEDA
	public static final int NUM_COLUMNS_SEARCH = 21;
	public static final String COLUMN_MOLEC_INDEX = "Index";
	public static final String COLUMN_MOLEC_CATALOG = "Catalog";
	public static final String COLUMN_MOLEC_MOLECULE = "Molecul";
	public static final String COLUMN_MOLEC_DELTA_N = "Max_Delta_N";
	public static final String COLUMN_MOLEC_ELOW = "Elow";
	public static final String COLUMN_MOLEC_CM_1 = "CM-1";
	public static final String COLUMN_MOLEC_LOG = "Log10";
	public static final String COLUMN_MOLEC_RANGE_INI = "RANGE_MIN";
	public static final String COLUMN_MOLEC_RANGE_END = "RANGE_MAX";
	public static final String COLUMN_MOLEC_RANGE_WHERE = "RANGE_WHERE";
	public static final String COLUMN_MOLEC_RANGE_UNIT = "RANGE_UNIT";	
	public static final String COLUMN_MOLEC_FILE_MADCUBAIJ = "FILE_MADCUBEIJ";	
	public static final String COLUMN_MOLEC_TYPE_FILE = "TYPE_FILE";	
	public static final String COLUMN_MOLEC_RANGE_ROI = "ROWS_PIXEL";	
	public static final String COLUMN_MOLEC_RESOLUTION = "RESOLUTIONI";	
	public static final String COLUMN_MOLEC_BEAMSIZE = "BEAMSIZE";	
	public static final String COLUMN_MOLEC_NAME_TAB = "NAME_TAB";	
	public static final String COLUMN_MOLEC_LAST_INDEX = "LAST_INDEX";
	public static final String COLUMN_MOLEC_WHERE_EXTRA = "WHERE_EXTRA";
	public static final String COLUMN_MOLEC_RENAME_MOLECULE = "RENAME_MOLEC";	
	public static final String COLUMN_MOLEC_CUBES_SYNCHRO = "CUBES_SYNCHRO";	
	public static final int POS_COLUMN_MOLEC_INDEX = 0;
	public static final int POS_COLUMN_MOLEC_MOLECULE = 1;
	public static final int POS_COLUMN_MOLEC_CATALOG = 2;
	public static final int POS_COLUMN_MOLEC_ELOW = 3;
	public static final int POS_COLUMN_MOLEC_CM_1 = 4;//
	public static final int POS_COLUMN_MOLEC_LOG = 5;
	public static final int POS_COLUMN_MOLEC_DELTA_N = 6;
	public static final int POS_COLUMN_MOLEC_RANGE_INI = 7;	
	public static final int POS_COLUMN_MOLEC_RANGE_END = 8;	
	public static final int POS_COLUMN_MOLEC_RANGE_WHERE = 9;	
	public static final int POS_COLUMN_MOLEC_FILE_MADCUBAIJ = 11;
	public static final int POS_COLUMN_MOLEC_TYPE_FILE = 12;
	public static final int POS_COLUMN_MOLEC_RANGE_ROI = 13;
	public static final int POS_COLUMN_MOLEC_RESOLUTION = 14;
	public static final int POS_COLUMN_MOLEC_BEAMSIZE = 15;
	public static final int POS_COLUMN_MOLEC_NAME_TAB = 16;	
	public static final int POS_COLUMN_MOLEC_LAST_INDEX = 17;
	public static final int POS_COLUMN_MOLEC_WHERE_EXTRA = 18;	
	public static final int POS_COLUMN_MOLEC_RENAME_MOLECULE= 19;	
	public static final int POS_COLUMN_MOLEC_CUBES_SYNCHRO= 20;	
	
	//CRITERIOS TABLE SEARCH MOLECULES
	public static final int POS_COLUMN_TMOLEC_ELOW = 2;
	public static final int POS_COLUMN_TMOLEC_CM_1 = 3;
	public static final int POS_COLUMN_TMOLEC_LOG = 4;
	public static final int POS_COLUMN_TMOLEC_DELTA_N = 5;
	
	public static final int NUM_COLUMN_TMOLEC = 6;
	//COLUMN INDEX LETD
	public static final int POS_COLUMN_STARTABLE_INDEX = 0;
	public static final int POS_COLUMN_SPECTRAL_INDEX = 0;
	
	//TYPE SAVE STARTABLE
	public static final int SAVE_STARTABLE_ALL = 0;
	public static final int SAVE_STARTABLE_SPECTRAL = 1;
	public static final int SAVE_STARTABLE_LTED = 2;
	public static final int SAVE_STARTABLE_LTES = 3;
	public static final int SAVE_STARTABLE_GAUSS = 4;
	public static final int SAVE_STARTABLE_INTEGRATED = 5;
	public static final int SAVE_STARTABLE_FIT_SIMULATE = 6;
	public static final int SAVE_STARTABLE_FIT_AUTO = 7;
	
	//LABELS COLUMNS
	public static final int NUM_COLUMNS_PHYS_FILE_NEW = 10;
	public static final int NUM_COLUMNS_FITTING_FILE_NEW = 7;
	public static final int NUM_COLUMNS_PHYS_TABLE_OLD = 18;
	public static final int NUM_COLUMNS_FITTING_TABLE_OLD = 15;
	public static final int NUM_COLUMNS_PHYS_TABLE_NEW = 22;
	public static final int NUM_COLUMNS_FITTING_TABLE_NEW = 14;
	public static final int NUM_COLUMNS_QN = 7;
	public static final int NUM_COLUMNS_QNN = 7;
	public static final int NUM_COLUMNS_SPECTRAL_WITHOUT_QN = 16;//17 cuando añada otra vez columnarotor
	public static final int NUM_COLUMNS_SPECTRA_FILE = NUM_COLUMNS_SPECTRAL_WITHOUT_QN+NUM_COLUMNS_QN+NUM_COLUMNS_QNN;
	
	//public static final int NUM_COLUMNS_ALLS = 28;
	
	//COLUMNS Physical Parameters
	//--JPL-CMDS
	public static final String COLUMN_N_NEW = "N/EM";
	public static final String COLUMN_DELTA_N_NEW = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_N_NEW;
	public static final String COLUMN_DELTA_MAC_N_NEW = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_N_NEW;
	

	public static final String COLUMN_N_OLD = "N";
	public static final String COLUMN_DELTA_N_OLD = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_N_OLD;
	public static final String COLUMN_DELTA_MAC_N_OLD = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_N_OLD;
	
	public static final String COLUMN_TEX_NEW = "Tex/Te";
	public static final String COLUMN_DELTA_TEX_NEW = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TEX_NEW;
	public static final String COLUMN_DELTA_MAC_TEX_NEW = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TEX_NEW;
	public static final String COLUMN_TEX_OLD = "Tex";
	public static final String COLUMN_DELTA_TEX_OLD = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TEX_OLD;
	public static final String COLUMN_DELTA_MAC_TEX_OLD = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TEX_OLD;
	public static final String COLUMN_TBG = "Tbg";
	public static final String COLUMN_TAU_NEW = MyConstants.LABEL_TAU_NO_MAC_OLD+"/"+MyConstants.LABEL_TAU_NO_MAC_OLD+"l";
	public static final String COLUMN_TAU_NEW_MAC = MyConstants.LABEL_TAU_MAC_OLD+"/"+MyConstants.LABEL_TAU_MAC_OLD+"l";
	public static final String COLUMN_DELTA_TAU_NEW = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TAU_NEW;
	public static final String COLUMN_DELTA_TAU_NEW_MAC = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TAU_NEW_MAC;
	public static final String COLUMN_DELTA_TAU_OLD_NO_MAC = MyConstants.LABEL_DELTA_NO_MAC+" "+MyConstants.LABEL_TAU_NO_MAC_OLD;
	public static final String COLUMN_DELTA_TAU_OLD_MAC = MyConstants.LABEL_DELTA_MAC+" "+MyConstants.LABEL_TAU_MAC_OLD;
	public static final String COLUMN_T = "T";
	//Q NLTE
	//20240212 NUEVAS COLUMNAS 	qlogN, delta qlogN,, nLTE, delta nLTE, nqLTE, delta nqLTE, Tk, delta TK
	public static final String COLUMN_Q_LOGN = "qlogN";
	public static final String COLUMN_DELTA_Q_LOGN = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_Q_LOGN;
	public static final String COLUMN_DELTA_MAC_Q_LOGN = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_Q_LOGN;
	public static final String COLUMN_NLTE = "nLTE";
	public static final String COLUMN_DELTA_NLTE = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_NLTE;
	public static final String COLUMN_DELTA_MAC_NLTE = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_NLTE;
	public static final String COLUMN_NQLTE = "nqLTE";
	public static final String COLUMN_DELTA_NQLTE = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_NQLTE;
	public static final String COLUMN_DELTA_MAC_NQLTE = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_NQLTE;
	public static final String COLUMN_TK = "Tk";
	public static final String COLUMN_DELTA_TK = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TK;
	public static final String COLUMN_DELTA_MAC_TK = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TK;
	
	//--RECOMB. LINES
	public static final String COLUMN_n_RECOMB = "n";
	public static final String COLUMN_m_RECOMB = "m";
	public static final String COLUMN_DELTA_n_RECOMB_MAC = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_n_RECOMB;
	public static final String COLUMN_DELTA_n_RECOMB = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_n_RECOMB;
	
	//DEsAPARECE Y SE COMBIERTE EN N/EM
	public static final String COLUMN_EM_OLD = "EM";
	public static final String COLUMN_DELTA_EM_OLD = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_EM_OLD;
	public static final String COLUMN_DELTA_MAC_EM_OLD = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_EM_OLD;

	//DEsAPARECE Y SE COMBIERTE EN Tex/Te (Con asterisco ver como ponerlo)
	public static final String COLUMN_TE_OLD = "Te";
	public static final String COLUMN_DELTA_TE_OLD = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TE_OLD;
	public static final String COLUMN_DELTA_MAC_TE_OLD = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TE_OLD;
	
	public static final String COLUMN_TAU_C_RECOMB = MyConstants.LABEL_TAU_NO_MAC_OLD+"c";
	public static final String COLUMN_TAU_C_RECOMB_MAC = MyConstants.LABEL_TAU_MAC_OLD+"c";
	public static final String COLUMN_DELTA_TAU_C_RECOMB = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TAU_C_RECOMB;
	public static final String COLUMN_DELTA_TAU_C_RECOMB_MAC = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TAU_C_RECOMB_MAC;

	//DEsAPARECE Y SE COMBIERTE EN Tau/Taul (Con sub indice la l ver como hacerlo)
	public static final String COLUMN_TAU_L_RECOMB_OLD = MyConstants.LABEL_TAU_NO_MAC_OLD+"l";
	public static final String COLUMN_TAU_L_RECOMB_OLD_MAC = MyConstants.LABEL_TAU_MAC_OLD+"l";
	public static final String COLUMN_DELTA_TAU_L_RECOMB_OLD = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_TAU_L_RECOMB_OLD;
	public static final String COLUMN_DELTA_TAU_L_RECOMB_OLD_MAC = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_TAU_L_RECOMB_OLD_MAC;
	
	//COLUMNS Fitting Parameters
	public static final String COLUMN_SOURCE = "Source";
	public static final String COLUMN_X = "x";
	public static final String COLUMN_Y = "y";
	public static final String COLUMN_C = "C";
	public static final String COLUMN_TYPE_C_OLD = "Type C.";
	public static final String COLUMN_AREA = "Area";
	public static final String COLUMN_WIDTH = "Width";
	public static final String COLUMN_DELTA_WIDTH = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_WIDTH;
	public static final String COLUMN_DELTA_MAC_WIDTH = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_WIDTH;
	public static final String COLUMN_VELOCITY = "Velocity";
	public static final String COLUMN_DELTA_VELO = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_VELOCITY;
	public static final String COLUMN_DELTA_MAC_VELO = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_VELOCITY;
	public static final String COLUMN_INTENSITY = "Intensity";
	public static final String COLUMN_SOURCESIZE = "SourceSize";
	public static final String COLUMN_DELTA_SOURCES = MyConstants.LABEL_DELTA_NO_MAC+" "+COLUMN_SOURCESIZE;
	public static final String COLUMN_DELTA_MAC_SOURCES = MyConstants.LABEL_DELTA_MAC+" "+COLUMN_SOURCESIZE;
	public static final String COLUMN_TELESCOPE = "Telescope";	
	public static final String COLUMN_CONTINUUM= "Continuum";	
	public static final String COLUMN_DELTA_CONTINUUM = MyConstants.LABEL_DELTA_NO_MAC+" "+ COLUMN_CONTINUUM;	
	public static final String COLUMN_DELTA_MAC_CONTINUUM=MyConstants.LABEL_DELTA_MAC+" "+ COLUMN_CONTINUUM;	
	public static final String COLUMN_CHECK = "CHECK";	
	public static final String COLUMN_CONTINUUM_SIZE= "Cont.Size";
	public static final String COLUMN_ABSORTION_FRACTION= "Fraction";
	public static final String COLUMN_PURGE_SIM_FIT= "Purge";
	public static final String COLUMN_FILTER_SIM_FIT= "Filter";
	public static final String COLUMN_NOISE_SIM_FIT= "Noise";
	public static final String COLUMN_LOG_N_WITHOUT_HTML_SIM_FIT= "logN|EM";
	public static final String COLUMN_TEX_WITHOUT_HTML_SIM_FIT= "Tex|Te*";

	//COLUMNS FORM SPECTRAL
	public static final String COLUMN_DFREQ = "err";
	public static final String COLUMN_DRFREE = "dr";
	public static final String COLUMN_GUP = "gup";
	public static final String COLUMN_TAG = "tag";	
	public static final String COLUMN_CATALOG = "cata";	
	public static final String COLUMN_QLOG300 = "qlog300";
	public static final String COLUMN_QLOG225 = "qlog225";
	public static final String COLUMN_QLOG150 = "qlog150";
	public static final String COLUMN_QLOG75 = "qlog75";
	public static final String COLUMN_QLOG37 = "qlog37";
	public static final String COLUMN_QLOG18 = "qlog18";
	public static final String COLUMN_QLOG9 = "qlog9";
//	public static final String COLUMN_REPLACE_QLOG = "rep_qlog";
	public static final String COLUMN_QNFMT = "qnfmt";	
	//COLUMNS Spectral Parameters
	public static final String COLUMN_TRANSITION = "Transition";	
	public static final String COLUMN_LGINT = "LgInt";	
	public static final String COLUMN_ELO = "ELO";	
	public static final String COLUMN_FREQUENCY = "Frequency";	
	public static final String COLUMN_FORMULA = "Formula";	
	public static final String COLUMN_QN = "qn";
	public static final String COLUMN_QNN = "qnn";
	
	public static final String COLUMN_INDEX_SPEC_OLD = "Index_Spec";
	public static final String COLUMN_INDEX = "Index";
	//COLUMNS DATA SIMULATE
	public static final String COLUMN_ISFILTERED = "Filtered";

	
	
	//COMPONENTS
	public static final String COMPONENT_GAUSS = "GA";
	public static final String COMPONENT_PRINTAREA = "IN";
	public static final String COMPONENT_SIMULATION = "SI";	
	public static final String COMPONENT_AUTOFIT = "Fit";	

	public static final String COMPONENT_NAME_TAB_GAUSS = "GAUSS";
	public static final String COMPONENT_NAME_TAB_PRINTAREA = "INTEGRATED";
	public static final String COMPONENT_NAME_TAB_SIMULATION = "SIMULATE";	
	public static final String COMPONENT_NAME_TAB_AUTOFIT = "FIT";		
	public static final String COMPONENT_NAME_TAB_ORIGIN = "Orig";	
	public static final String COMPONENT_NAME_TAB_AUX = "AUX.";
	

	public static final String COMPONENT_NAME_EXT_FILE_GAUSS = "GAUSS";
	public static final String COMPONENT_NAME_EXT_FILE_PRINTAREA = "INT";
	public static final String COMPONENT_NAME_EXT_FILE_SIMULATION = "SIM";	
	public static final String COMPONENT_NAME_EXT_FILE_AUTOFIT = "FIT";		
	public static final String COMPONENT_NAME_EXT_FILE_ORIGIN = "LETD";	
	public static final String COMPONENT_NAME_EXT_FILE_AUX = "AUX";
	
	//TYPE FILE MADCUBA_IJ
	public static final String FITS_TYPE_CUBE = "CUBE";	
	public static final String FITS_TYPE_CUBE_SYNCHRO = "CUBE_SYN";	
	public static final String FITS_TYPE_SPECTRAL = "SPECTRA";
	public static final String NAME_FITS_TYPE_CUBE_SYNCHRO_OLD = "SYNCHRONIZE_CUBE";
	public static final String NAME_FITS_TYPE_CUBE_SYNCHRO_NEW = "synchro_cubes.cub";
	public static final String NAME_FITS_TYPE_CUBE_SYNCHRO_DIRECTORY = "synchro_cubes";
	
	//FILE EXTENSION
	//public static final String FILE_EXTENSION_SLIM = "mad.tar";
	public static final String FILE_EXTENSION_SLIM_V1 = "mad.zip";
	public static final String FILE_EXTENSION_SLIM_V2 = "slim";
	public static final String FILE_SEARCH = "search";
	public static final String FILE_SEARCH_OLD = "Criterios";
	public static final String FILE_SLIM_EXTENSION_NAME_BASE = ".info";
	public static final String DIRECTORY_EXTENSION_TEMP_SLIM = "_temp_slim";
	public static final String DIRECTORY_EXTENSION_TEMP_SLIM_VO = "_temp_slim_vo";
	public static final String DIRECTORY_EXTENSION_TEMP = "_temp";
	public static final String DEFAULT_DIRECTORY_SEARCH = "search_new";//POR DEFECTO COMO SE VA A LLAMR AL DIRECTORIO DE LA ESTRUCTURA DE LOS FICHEROS
	public static final String FILE_SLIM_INTENSITY_COMPLETE = "_int";
	public static final String FILE_SLIM_INTENSITY_CHECK = "_int_check";
	public static final String FILE_SLIM_CONTINUUM = "CONTINUUM";
	public static final String FILE_SLIM_EXTENSION_HISTORY = ".hist";
	public static final String FILE_SLIM_EXTENSION_ENERGY = ".egy";
	public static final String FILE_NAME_SOURCE_CATALOG_ANALYSIS = "PARAMS_SOURCE_QNLTE.dat";
	public static final String FILE_NAME_DATA_CATALOG_ANALYSIS = "PARAMS_CATALOG_QNLTE.dat";
	public static final String FILE_NAME_DATA_CATALOG_ANALYSIS_OLD = "PARAMS_QNLTE.dat";
	public static final String DIRECTORY_POSITION_SEARCH = "search";//AQUEI SE ENCUENTRAN SIEMPREN TODAS LAS MOLECULES QUE SE HAN BUSQCADO
	//(DIRECTORIO NOMBRE MOLECULA O TAG_CATALOG MAS NOMBRE MOLECULA O TAG_CATALOG.fits)
	public static final String DIRECTORY_POSITION_CUBES = "cubes";//AQUI VAMOS A GUARDAR LOS CUBOS DE LOS PARAMETROS PARA CADA PESTAÑA

	public static final String DIRECTORY_DEFAULT_PIXEL = "000_000"; //POR DEFECTO LOS PIXEL ES ESTO (ES PARA LOS ESPECTROS)

	
	
	//FUNCTIONS TYPE_REPLACE
	public static final String FUNCTIONS_REPLACE_NAME_CANCEL = "Cancel";
	public static final String FUNCTIONS_REPLACE_NAME_REPLACE = "Replace";
	public static final String FUNCTIONS_REPLACE_NAME_ADD = "ADD";
	public static final int FUNCTIONS_REPLACE_CANCEL = 0;
	public static final int FUNCTIONS_REPLACE_REPLACE = 2;
	public static final int FUNCTIONS_REPLACE_ADD = 1;
	
	//PLOT SHOW
	public static final int PLOT_SHOW_SAME_WINDOW = 0;
	public static final int PLOT_SHOW_DIFERRENT_WINDOWS = 1;
	public static final int PLOT_SHOW_NONE = 2;
	
	//SLIM FIXED
	public static final float ERROR_FIXED_DEFAULT = -1F;
	public static final float ERROR_FIXED_DEFAULT_OLD = -1000F;

	public static final float AUTOFIT_NO_CONVERT_DEFAULT = -100F;
	
	//CONTINUUM
//	public static final int NUM_COLUMNS_CONTINUUM = 1;

	public final static int CONTINUUM_COLUMN_POS_TYPE_MODIFY = 21;
	
	public static final String CONTINUUM_TABLE_FT = "TABLE_FT";
	public static final String CONTINUUM_FILE_NAME = "NAME_FILE";
	public static final String CONTINUUM_FILE_FF = "FILE_FF";
	public static final String CONTINUUM_CMB_TBG = "C_TBG";
	public static final String CONTINUUM_MODEL_BLACK_TBB = "B_TBB";
	public static final String CONTINUUM_MODEL_BLACK_SBB = "B_SBB";
	public static final String CONTINUUM_MODEL_BLACK_FBB = "B_FBB";
	public static final String CONTINUUM_MODEL_GREY_TGB = "G_TGB";
	public static final String CONTINUUM_MODEL_GREY_SGB = "G_SGB";
	public static final String CONTINUUM_MODEL_GREY_FGB = "G_FGB";
	public static final String CONTINUUM_MODEL_GREY_TAUGB = "G_TAU";
	public static final String CONTINUUM_MODEL_GREY_NUGB = "G_NU";
	public static final String CONTINUUM_MODEL_GREY_BETAGB = "G_BETA";
	public static final String CONTINUUM_MODEL_POWER_JPW = "P_JPW";
	public static final String CONTINUUM_MODEL_POWER_SPW = "P_SPW";
	public static final String CONTINUUM_MODEL_POWER_FPW = "P_FPW";
	public static final String CONTINUUM_MODEL_POWER_NUPW = "P_NU";
	public static final String CONTINUUM_MODEL_POWER_ALFAPW = "P_ALFA";
	
	public static final int TYPE_PRODUCT_CONTINUUM_BASELINE = 0;
	public static final int TYPE_PRODUCT_CONTINUUM_TABLE = 1;
	public static final int TYPE_PRODUCT_CONTINUUM_CMB = 2;
	public static final int TYPE_PRODUCT_CONTINUUM_BLACK = 3;
	public static final int TYPE_PRODUCT_CONTINUUM_GREY = 4;
	public static final int TYPE_PRODUCT_CONTINUUM_POWER = 5;
	public static final int TYPE_PRODUCT_CONTINUUM_FILE = 6;
	
	//SIGLAS
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_TABLE="TA";
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_FILE="FI";
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_BASELINE="BA";
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_BLACK="BL";
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_GREY="GR";
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_POWER="PO";
	public static final String TYPE_MODIF_PRODUCT_CONTINUUM_CMB="CMB";
	
	//DESCRIPTION
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_FILE="FILE";
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_BASELINE_1="BASE";
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_BASELINE_2="BASELINE";
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_BLACK="BLACK";
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_GREY="GREY";
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_POWER="POWER";
	public static final String TYPE_DESCRIP_PRODUCT_CONTINUUM_CMB="CMB";
	
	//CONSTANCE CALCULATE SLIM

	public static double K = 1.3807000E-16;  // erg K-1
	public static double H = 6.6261003E-27;  // erg s
	public static double C = 2.9979247E+10;  // cm/s
	public static double HCK = H*C/K;
	public static double RAD2ARCSEC = 206264.8062471; 
	public static double M_E_SI = 9.10938215E-31;  // electron mass in kg(NIST)

	public static double K_SI = 1.3806504E-23;  //  Boltzmann constant (NIST)
	public static double H_SI = 6.62606896E-34;  //  Planck  constant (NIST)
	
	
	//CONSTANT MAKE PLOT SIMULATION
	public static final String HEADER_OBSERVER_SLIM ="SLIM_MADCUBAIJ";
	public static final String HEADER_OBJECT_SLIM = "SIMULATION";
	//CUBES PARAMETERS FROM SLIM
	public static final String HEADER_SLIM = MyConstants.HEADER_SLIM_SLIM;
	
	//TYPE ORDER IN SIMULATE (SIMFIT)
	public static final int TYPE_ORDER_SIMFIT_FREQ = 1; //VALUE DEFAULT
	public static final int TYPE_ORDER_SIMFIT_LOGINT = 2; 
	public static final int TYPE_ORDER_SIMFIT_ELO_DOWN = 3; 
	public static final int TYPE_ORDER_SIMFIT_INTENSITY = 4; 
	public static final int TYPE_ORDER_SIMFIT_ELO_UP = 5; 
	
	
	//MESSAGE
	public static final String MESSAGGE_ISMAKEAUTOFIT = "Component(s) already fitted. Do you want to continue?";
	public static final String MESSAGGE_ISMAKEAUTOFIT_DEBUG = "Component already fitted. Lost mark.";
	public static final String MESSAGGE_TITLE_ISMAKEAUTOFIT ="Previus Autofit";
	public static final String CATALOG_RECOMB_DIRECTORY = "RECOMB";
	public static final String SEPARATOR_QN = ",";
	public static final String SEPARATOR_QN_QNN = "-";
	
	//DEPRECATED PLUGINS

	public static final String SLIM_SET_SAME_MOLECULE_OLD = "SLIM_Set_Same_Molecule";
	
}
