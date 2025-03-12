package es.cab.plugins.impor;

import static es.cab.plugins.impor.ImportConst.*;

import java.util.HashMap;

public class RowImportCubeJWST extends RowImport{

    @Override
    protected void initKeyStoreInteger() {
        keyStoreInteger = new HashMap<String, Integer>();

        keyStoreInteger.put(SCAN, 5);
        keyStoreInteger.put(OBS_NUM, 6);
        keyStoreInteger.put(OBSNUMBER, 6);
        // keyStoreInteger.put(CRPIX1, 3);
        // keyStoreInteger.put(CRPIX2, 4);
        keyStoreInteger.put(VELREF, 35);
        keyStoreInteger.put(CHANNELS, 40);
        keyStoreInteger.put(QUALITY, 53);
        keyStoreInteger.put(NAXIS3, 40);
        keyStoreInteger.put(NAXIS2, 76);
        keyStoreInteger.put(NAXIS1, 77);
        keyStoreInteger.put(BITPIX, 78);
		keyStoreInteger.put(CDELT4, 79);
        keyStoreInteger.put(NAXIS, 80);
        keyStoreInteger.put(WCSAXES, 80);
        
    }

    @Override
    protected void initKeyStoreFloat() {
        keyStoreFloat = new HashMap<String, Integer>();

        keyStoreFloat.put(EXPOSURE, 11);
        keyStoreFloat.put(INTEGTIM, 11);
        keyStoreFloat.put(AZIMUTH, 13);
        keyStoreFloat.put(ELEVATIO, 14);
        keyStoreFloat.put(EQUINOX, 27);
        keyStoreFloat.put(EPOCH, 27);
        keyStoreFloat.put(BMAJ, 45);
        keyStoreFloat.put(BMIN, 46);
        keyStoreFloat.put(BPA, 47);
        keyStoreFloat.put(APEREFF, 55);
        keyStoreFloat.put(ETAA, 55);
        keyStoreFloat.put(BEAMEFF, 56);
        keyStoreFloat.put(ETAMB, 56);
        keyStoreFloat.put(ETAFFS, 57);
        keyStoreFloat.put(ETAL, 57);
        keyStoreFloat.put(FORWARDEFF, 57);
        keyStoreFloat.put(ANTGAIN, 58);
        keyStoreFloat.put(GAINIMAG, 59);
        keyStoreFloat.put(GAINIMAGE, 59);
        keyStoreFloat.put(BLANKING, 67);
        keyStoreFloat.put(SIGMA, 70);
    }

    @Override
    protected void initKeyStoreDouble() {
        keyStoreDouble = new HashMap<String, Integer>();

        keyStoreDouble.put(TIME_OBS, 8);
        //keyStoreDouble.put(OBS_TIME, 8);
        keyStoreDouble.put(OBSTIME, 8);
        keyStoreDouble.put(LST, 9);
        // keyStoreDouble.put(CRPIX3, 18);
        keyStoreDouble.put(CRVAL3, 19);
        keyStoreDouble.put(CDELT3, 20);
        keyStoreDouble.put(CRVAL1, 22);
        keyStoreDouble.put(CDELT1, 23);
        keyStoreDouble.put(CRVAL2, 25);
        keyStoreDouble.put(CDELT2, 26);
        keyStoreDouble.put(YOFFSET, 26);
        keyStoreDouble.put(RESTFRQ, 29);
        keyStoreDouble.put(RESTFREQ, 29);
        keyStoreDouble.put(IMAGFREQ, 30);
        keyStoreDouble.put(IMAGEFREQ, 30);
        keyStoreDouble.put(SPECRES, 31);
        keyStoreDouble.put(FREQRES, 31);
        keyStoreDouble.put(ALTRPIX, 36);
        keyStoreDouble.put(ALTRVAL, 37);
        keyStoreDouble.put(ZSOURCE, 38);
        keyStoreDouble.put(REDSHIFT, 38);
        keyStoreDouble.put(RESTWAV, 60);
        keyStoreDouble.put(RESTWAVE, 60);
        keyStoreDouble.put(CRPIX1, 72);
        keyStoreDouble.put(CRPIX2, 73);
        keyStoreDouble.put(CRPIX3, 74);
//        keyStoreDouble.put(CDELT4, 79);

    }
    
    @Override
    protected void initKeyStoreArrFloat() {
        keyStoreArrFloat = new HashMap<String, Integer>();

        keyStoreArrFloat.put(DATA, 41);
        keyStoreArrFloat.put(WEIGHTS, 42);
        keyStoreArrFloat.put(WAVE, 43);
    }

    
    @Override
    protected void initKeyStoreString() {
        keyStoreString = new HashMap<String, Integer>();

        keyStoreString.put(PROJID, 0);
        keyStoreString.put(OBS_ID, 0);
        keyStoreString.put(PROJECTID, 0);
        keyStoreString.put(OBSERVER, 1);
        keyStoreString.put(VERSION, 2);
        keyStoreString.put(DATE_OBS, 7);
        keyStoreString.put(OBSDATE, 7);
        keyStoreString.put(DATE, 10);
        keyStoreString.put(OBJECT, 12);         
        keyStoreString.put(HISTORY, 15);
        keyStoreString.put(ORIGIN, 16);
        keyStoreString.put(CTYPE3, 17);
        
        keyStoreString.put(XLABEL, 75);
        keyStoreString.put(CTYPE1, 21);
        keyStoreString.put(CTYPE2, 24);
        keyStoreString.put(RADESYS, 28);
        keyStoreString.put(MOLECULE, 32);
        keyStoreString.put(TRANSITION, 33);
        keyStoreString.put(VELDEF, 34);
        keyStoreString.put(STOKES, 39);
        keyStoreString.put(POLARIZATION, 39);
        keyStoreString.put(TELESCOP, 44);
        keyStoreString.put(TELESCOPE, 44);
        keyStoreString.put(FRONTEND, 48);
        keyStoreString.put(INSTRUME, 48);
        keyStoreString.put(BACKEND, 49);
        keyStoreString.put(OBSMODE, 52);
        keyStoreString.put(OBS_MODE, 52);
//        keyStoreString.put(TUNIT10, 54);
        keyStoreString.put(BUNIT, 54);
//        keyStoreString.put(UNITINTEN, 54);
        keyStoreString.put(HIST_OLD, 61);
        keyStoreString.put(TEMPSCAL, 62);
        keyStoreString.put(TEMPSCALE, 62);
        // Unidades no incorporadas al row[]
        keyStoreString.put(CUNIT1, 63);
        keyStoreString.put(CUNIT2, 63);
        keyStoreString.put(UNITANGLE, 63);
        keyStoreString.put(CUNIT3, 64);
        keyStoreString.put(WAVEUNIT, 64);
//        keyStoreString.put(TUNIT9, 64);
        keyStoreString.put(UNITFREQ, 64);
        keyStoreString.put(UNITSPECTRAL, 64);
        keyStoreString.put(UNITVELO, 65);
        keyStoreString.put(UNITTIME, 66);
        keyStoreString.put(COORDTYPE, 68);
        keyStoreString.put(COORDPROJ, 69);
        keyStoreString.put(SPECSYS, 71);
    }
}