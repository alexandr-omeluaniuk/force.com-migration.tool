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
package ss.fmt.command;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ss.fmt.constants.ProjectFile;
import ss.fmt.constants.ProjectFolder;
import ss.fmt.jaxb.model.CustomLabel;
import ss.fmt.jaxb.model.CustomLabels;
import ss.fmt.jaxb.model.Translations;
import ss.fmt.util.CustomLabelsUtil;
import ss.lana.api.CommandArgument;
import ss.lana.api.CommandExecutor;

/**
 * Export custom labels translations.
 * @author ss
 */
@Component
class CustomLabelsExport extends CustomLabelsUtil
        implements CommandExecutor {
    /** Logger. */
    private static final Logger LOG = Logger
            .getLogger(CustomLabelsExport.class);
// ============================= ARGS =========================================
    /** Salesforce project 'src' folder path. */
    private static final String ARG_PROJECT_PATH = "project-src";
    /** Exported languages. */
    private static final String ARG_LANGUAGES = "languages";
    /** Filter by category. */
    private static final String ARG_CATEGORY = "category";
    /** Filter by prefix. */
    private static final String ARG_PREFIX = "prefix";
// ============================================================================
    @Override
    public String name() {
        return "custom-labels-export";
    }
    @Override
    public String description() {
        return "export custom labels to external format (xlsx)";
    }
    @Override
    public void execute(List<CommandArgument> args) throws Exception {
        final Map<String, String> values = new HashMap<>();
        args.stream().forEach((arg) -> {
            values.put(arg.getName(), arg.getValue());
        });
        String projectPath = values.get(ARG_PROJECT_PATH);
        String languages = values.get(ARG_LANGUAGES);
        String category = values.get(ARG_CATEGORY);
        String prefix = values.get(ARG_PREFIX);
        LOG.info("project absolute path [" + projectPath + "]");
        LOG.info("export languages [" + languages + "]");
        if (category != null) {
            LOG.info("filter by category [" + category + "]");
        }
        if (prefix != null) {
            LOG.info("filter by prefix [" + prefix + "]");
        }
        File customLabelsFile = new File(projectPath + File.separator
                + ProjectFolder.CUSTOM_LABELS + File.separator
                + ProjectFile.CUSTOM_LABELS);
        if (customLabelsFile.exists()) {
            LOG.info(customLabelsFile.getAbsoluteFile() + " found");
        } else {
            LOG.fatal(ProjectFile.CUSTOM_LABELS + " not exist! Path ["
                    + customLabelsFile.getAbsolutePath() + "]");
        }
        Map<String, Map<String, String>> translationFiles = new HashMap<>();
        for (String lang : languages.split(",")) {
            File f = new File(projectPath + File.separator
                    + ProjectFolder.TRANSLATIONS + File.separator
                    + String.format(ProjectFile.TMPL_TRANSLATION, lang.trim()));
            if (f.exists()) {
                LOG.info("translations for '" + lang.trim() + "' found. Path ["
                        + f.getAbsolutePath() + "]");
                Translations t = extractTranslations(f);
                Map<String, String> map = new HashMap<>();
                t.getCustomLabels().forEach((cl) -> {
                    map.put(cl.getName(), cl.getLabel());
                });
                translationFiles.put(lang, map);
            } else {
                LOG.warn("translations for '" + lang.trim() + "' not found. "
                        + "Path [" + f.getAbsolutePath() + "]");
            }
        }
        // Unmarshall files
        CustomLabels customLabels = extractCustomLabels(customLabelsFile);
        final Map<String, String> labelsMap = new HashMap<>();
        customLabels.getLabels().stream().forEach((l) -> {
            labelsMap.put(l.getFullName(), l.getShortDescription());
        });
        List<List<String>> table = createTableData(
                translationFiles, customLabels, category, prefix);
        printTable(table);
        LOG.info("total rows [" + (table.size() - 1) + "]");
        exportXlsx(table);
    }
    @Override
    public Set<CommandArgument> arguments() {
        Set<CommandArgument> args = new HashSet<>();
        args.add(new CommandArgument(ARG_PROJECT_PATH,
                "path to project 'src' folder", true));
        args.add(new CommandArgument(ARG_LANGUAGES,
                "list of the languages separated by comma, "
                        + "example: 'en_US, de'", true));
        args.add(new CommandArgument(ARG_CATEGORY,
                "filter translations by custom label category", false));
        args.add(new CommandArgument(ARG_PREFIX,
                "filter translations by key prefix", false));
        return args;
    }
// ====================== PRIVATE =============================================
    /**
     * Create table data.
     * @param translationFiles translation files data.
     * @param customLabels custom labels data.
     * @param category filter by category.
     * @param prefix  filter by prefix.
     * @return table data.
     */
    private List<List<String>> createTableData(
            final Map<String, Map<String, String>> translationFiles,
            final CustomLabels customLabels,
            final String category, final String prefix) {
        List<List<String>> table = new ArrayList<>();
        List<String> firstRow = new ArrayList<>();
        firstRow.add("Translation key");
        firstRow.add("Description");
        table.add(firstRow);
        translationFiles.keySet().forEach((lang) -> {
            firstRow.add(lang);
        });
        for (CustomLabel cl : customLabels.getLabels()) {
            String key = cl.getFullName();
            // start of filter
            if (category != null && !category.equals(cl.getCategories())) {
                continue;
            }
            if (prefix != null && !key.startsWith(prefix)) {
                continue;
            }
            // end of filter
            List<String> row = new ArrayList<>();
            row.add(key);
            row.add(cl.getShortDescription());
            for (String lang : translationFiles.keySet()) {
                if (lang.equals(cl.getLanguage())) {
                    row.add(cl.getValue());
                } else {
                    Map<String, String> tMap = translationFiles.get(lang);
                    if (tMap.containsKey(cl.getFullName())) {
                        row.add(tMap.get(cl.getFullName()));
                    } else {
                        row.add("");
                    }
                }
            }
            table.add(row);
        }
        return table;
    }
    /**
     * Export data to XLSX file.
     * @param table data table.
     * @throws Exception error.
     */
    private void exportXlsx(final List<List<String>> table) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("custom labels");
        int counter = 0;
        CellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        for (List<String> dataRow : table) {
            sheet.setColumnWidth(counter, 10000);
            Row row = sheet.createRow(counter);
            for (int i = 0; i < dataRow.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(style);
                cell.setCellValue(dataRow.get(i));
            }
            row.setRowStyle(style);
            counter++;
        }
        File file = new File("custom-labels-export.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            wb.write(fileOut);
            LOG.info("translations saved in file [" + file.getAbsolutePath()
                    + "]");
        }
    }
}
