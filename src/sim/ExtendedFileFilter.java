/*
 *  This file is part of j8085sim.
 *
 *   j8085sim is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   j8085sim is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with j8085sim.  If not, see <http://www.gnu.org/licenses/>. 
 */

/*
 *   Developer: Sinu John - http://www.sinujohn.wordpress.com
 *               email: sinuvian@gmail.com
 *
 *    Copyright 2010 Sinu John
 */

package sim;
import java.io.File;
import javax.swing.filechooser.*;
/**
 *
 * @author sinu
 */
public class ExtendedFileFilter extends FileFilter{

    String description,extension;

    public ExtendedFileFilter(String desc, String ext) {
        description = desc;
        extension = ext;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean accept(File file)
    {
        if(file.isDirectory()) return true;
        String path=file.getAbsolutePath().toLowerCase();
        String ext="."+extension;
        if(path.endsWith(ext)) return true;
        return false;
    }

}
