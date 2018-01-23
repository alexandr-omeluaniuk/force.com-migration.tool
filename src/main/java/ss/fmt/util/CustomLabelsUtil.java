/*
 * The MIT License
 *
 * Copyright 2018 Pivotal Software, Inc..
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ss.fmt.util;

import java.io.File;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;
import ss.fmt.jaxb.model.CustomLabels;
import ss.fmt.jaxb.model.Translations;

/**
 * Custom labels utility.
 * @author ss
 */
public abstract class CustomLabelsUtil {
    /** Logger. */
    private static final Logger LOG = Logger.getLogger(CustomLabelsUtil.class);
    /**
     * Extract custom labels.
     * @param file custom labels file.
     * @return unmarshalled object.
     * @throws Exception error.
     */
    protected CustomLabels extractCustomLabels(final File file)
            throws Exception {
        JAXBContext jc = JAXBContext.newInstance(CustomLabels.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        CustomLabels object = (CustomLabels) unmarshaller.unmarshal(file);
        LOG.info("total custom labels found [" + object.getLabels().size()
                + "]");
        return object;
    }
    /**
     * Extract translations.
     * @param file translations file.
     * @return unmarshalled object.
     * @throws Exception error.
     */
    protected Translations extractTranslations(final File file)
            throws Exception {
        JAXBContext jc = JAXBContext.newInstance(Translations.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        Translations object = (Translations) unmarshaller.unmarshal(file);
        LOG.info("total translations found [" + object.getCustomLabels().size()
                + "]");
        return object;
    }
    /**
     * Print table.
     * @param table table with data.
     */
    protected void printTable(final List<List<String>> table) {
        if (table.isEmpty()) {
            LOG.warn("translation table is empty");
            return;
        }
        int colWidth = 30;
        StringBuilder sb = new StringBuilder();
        StringBuilder sbFmt = new StringBuilder("|");
        int columns = table.get(0).size();
        String[] cells = new String[columns];
        for (int i = 0; i < columns; i++) {
            sbFmt.append(" %-").append(colWidth).append("s |");
            cells[i] = new String(new char[colWidth]).replace('\0', '-');
        }
        sbFmt.append("\n");
        String format = sbFmt.toString();
        String hline = String.format(format.replace("|", "+"), cells);
        int counter = 0;
        for (List<String> row : table) {
            if (counter == 1 || counter == 0) {
                sb.append(hline);
            }
            String[] data = new String[row.size()];
            for (int i = 0; i < row.size(); i++) {
                String s = row.get(i);
                if (s.length() > colWidth - 3) {
                    data[i] = s.substring(0, colWidth - 3) + "...";
                } else {
                    data[i] = s;
                }
            }
            sb.append(String.format(format, data));
            counter++;
        }
        sb.append(hline);
        LOG.info("\n Print table \n" + sb.toString());
    }
}
