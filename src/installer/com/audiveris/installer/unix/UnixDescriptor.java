//----------------------------------------------------------------------------//
//                                                                            //
//                          U n i x D e s c r i p t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import com.audiveris.installer.Descriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Class {@code UnixDescriptor} implements Installer descriptor
 * for Linux Ubuntu (32 and 64 bits)
 *
 * @author Hervé Bitteur
 */
public class UnixDescriptor
        implements Descriptor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            UnixDescriptor.class);

    /** Specific prefix for application folders. {@value} */
    private static final String TOOL_PREFIX = "/" + COMPANY_ID + "/"
                                              + TOOL_NAME;

    /** Set of requirements for c/c++. */
    private static final Package[] cReqs = new Package[]{
        new Package("libc6", "2.15"),
        new Package("libgcc1", "4.6.3"),
        new Package("libstdc++6", "4.6.3")
    };

    /** Requirement for ghostscript. */
    private static final Package gsReq = new Package("ghostscript", "9.06");

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // getConfigFolder //
    //-----------------//
    @Override
    public File getConfigFolder ()
    {
        String config = System.getenv("XDG_CONFIG_HOME");

        if (config != null) {
            return new File(config + TOOL_PREFIX);
        }

        String home = System.getenv("HOME");

        if (home != null) {
            return new File(home + "/.config" + TOOL_PREFIX);
        }

        throw new RuntimeException("HOME environment variable is not set");
    }

    //---------------//
    // getDataFolder //
    //---------------//
    @Override
    public File getDataFolder ()
    {
        String data = System.getenv("XDG_DATA_HOME");

        if (data != null) {
            return new File(data + TOOL_PREFIX);
        }

        String home = System.getenv("HOME");

        if (home != null) {
            return new File(home + "/.local/share" + TOOL_PREFIX);
        }

        throw new RuntimeException("HOME environment variable is not set");
    }

    //--------------------------//
    // getDefaultTessdataPrefix //
    //--------------------------//
    @Override
    public File getDefaultTessdataPrefix ()
    {
        return new File("/usr/share/tesseract-ocr/");
    }

    //---------------//
    // getTempFolder //
    //---------------//
    @Override
    public File getTempFolder ()
    {
        final File folder = new File(getDataFolder(), "temp/installation");
        logger.debug("getTempFolder: {}", folder.getAbsolutePath());

        return folder;
    }

    //------------//
    // installCpp //
    //------------//
    @Override
    public void installCpp ()
            throws Exception
    {
        for (Package pkg : cReqs) {
            if (!pkg.isInstalled()) {
                pkg.install();
            }
        }
    }

    //--------------------//
    // installGhostscript //
    //--------------------//
    @Override
    public void installGhostscript ()
            throws Exception
    {
        gsReq.install();
    }

    //---------//
    // isAdmin //
    //---------//
    @Override
    public boolean isAdmin ()
    {
        return true; // TODO: implement this!
    }

    //----------------//
    // isCppInstalled //
    //----------------//
    @Override
    public boolean isCppInstalled ()
    {
        for (Package pkg : cReqs) {
            if (!pkg.isInstalled()) {
                return false;
            }
        }

        return true;
    }

    //------------------------//
    // isGhostscriptInstalled //
    //------------------------//
    @Override
    public boolean isGhostscriptInstalled ()
    {
        return gsReq.isInstalled();
    }

    //-----------------//
    // relaunchAsAdmin //
    //-----------------//
    @Override
    public void relaunchAsAdmin ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //--------//
    // setenv //
    //--------//
    @Override
    public void setenv (boolean system,
                        String var,
                        String value)
            throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}