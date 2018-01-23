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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ss.fmt.constants.ProjectFile;
import ss.fmt.constants.ProjectFolder;
import ss.fmt.jaxb.model.CustomLabels;
import ss.fmt.util.CustomLabelsUtil;
import ss.lana.api.CommandArgument;
import ss.lana.api.CommandExecutor;

/**
 * Import custom labels from xlsx file to metadata file.
 * @author ss
 */
@Component
class CustomLabelsImport extends CustomLabelsUtil implements CommandExecutor {
    /** Logger. */
    private static final Logger LOG = Logger
            .getLogger(CustomLabelsImport.class);
// ============================= ARGS =========================================
    /** Salesforce project 'src' folder path. */
    private static final String ARG_PROJECT_PATH = "project-src";
    /** Import file path. */
    private static final String ARG_IMPORT_FILE_PATH = "import-file-path";
// ============================================================================
    @Override
    public String name() {
        return "custom-labels-import";
    }
    @Override
    public String description() {
        return "import custom labels from external format (xlsx) "
                + "to metadata files";
    }
    @Override
    public void execute(final List<CommandArgument> args) throws Exception {
        final Map<String, String> values = new HashMap<>();
        args.stream().forEach((arg) -> {
            values.put(arg.getName(), arg.getValue());
        });
        String projectPath = values.get(ARG_PROJECT_PATH);
        String importFilePath = values.get(ARG_IMPORT_FILE_PATH);
        LOG.info("project absolute path [" + projectPath + "]");
        LOG.info("import file path [" + importFilePath + "]");
        File importFile = new File(importFilePath);
        if (!importFile.exists()) {
            LOG.fatal("import file not exist! Path ["
                    + importFile.getAbsolutePath() + "]");
            return;
        }
        File projectFolder = new File(projectPath);
        if (!projectFolder.exists()) {
            LOG.fatal("project folder not exist! Path ["
                    + projectFolder.getAbsolutePath() + "]");
            return;
        }
        List<List<String>> table = extractImportData(importFile);
        printTable(table);
        Map<String, Map<String, String>> langMap = createLanguageMap(table);
        writeChangesToMetadata(projectFolder, langMap);
    }
    @Override
    public Set<CommandArgument> arguments() {
        Set<CommandArgument> args = new HashSet<>();
        args.add(new CommandArgument(ARG_PROJECT_PATH,
                "path to project 'src' folder", true));
        args.add(new CommandArgument(ARG_IMPORT_FILE_PATH,
                "path to import file", true));
        return args;
    }
// ============================================================================
    /**
     * Extract xlsx file data.
     * @param file xlsx file.
     * @return table data.
     * @throws Exception error.
     */
    private List<List<String>> extractImportData(final File file)
            throws Exception {
        List<List<String>> table = new ArrayList<>();
        LOG.info("start read import file...");
        try (InputStream is = new FileInputStream(file)) {
            Workbook wb = new XSSFWorkbook(is);
            Sheet sheet = wb.getSheetAt(0);
            List<String> columns = new ArrayList<>();
            Row firstRow = sheet.getRow(sheet.getFirstRowNum());
            for (int k = firstRow.getFirstCellNum(); k <= firstRow.getLastCellNum(); k++) {
                Cell cell = firstRow.getCell(k);
                if (cell != null && cell.getStringCellValue() != null
                        && !cell.getStringCellValue().trim().isEmpty()) {
                    columns.add(cell.getStringCellValue());
                }
            }
            int columnsCount = columns.size();
            LOG.info("columns count [" + columnsCount + "]");
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                List<String> rowData = new ArrayList<>();
                for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
                    if (j >= columnsCount) {
                        continue;
                    }
                    Cell cell = row.getCell(j);
                    if (cell == null || cell.getStringCellValue() == null) {
                        rowData.add("");
                    } else {
                        rowData.add(cell.getStringCellValue());
                    }
                }
                table.add(rowData);
            }
        }
        LOG.info("import file was read...");
        return table;
    }
    /**
     * Reorder import data by language.
     * @param table import data.
     * @return import data for every language.
     */
    private Map<String, Map<String, String>> createLanguageMap(
            final List<List<String>> table) {
        Map<String, Map<String, String>> map = new HashMap<>();
        if (table.isEmpty()) {
            return map;
        }
        Map<Integer, String> langColumns = new HashMap<>();
        List<String> firstRow = table.get(0);
        if (firstRow.size() < 3) {
            LOG.fatal("invalid table structure! Forced exit...");
            return map;
        }
        for (int i = 0; i < firstRow.size(); i++) {
            if (i < 2) {
                continue;
            }
            String lang = firstRow.get(i);
            if (lang == null || lang.trim().isEmpty()) {
                continue;
            }
            langColumns.put(i, lang);
            map.put(lang, new HashMap<>());
            LOG.info("language found [" + lang + "]");
        }
        for (int i = 1; i < table.size(); i++) {
            List<String> row = table.get(i);
            if (row.size() < 2 + langColumns.size()) {
                LOG.warn("invalid row [" + i + "]");
                continue;
            }
            String tkey = row.get(0);
            for (int j = 2; j < row.size(); j++) {
                String langKey = langColumns.get(j);
                if (langKey != null && map.containsKey(langKey)) {
                    map.get(langKey).put(tkey, row.get(j));
                }
            }
        }
        return map;
    }
    /**
     * Write changes to metadata files.
     * @param projectFolder salesforce project folder.
     * @param langMap language map with translations.
     * @throws Exception error.
     */
    private void writeChangesToMetadata(final File projectFolder,
            final Map<String, Map<String, String>> langMap) throws Exception {
        File customLabelsFile = new File(projectFolder,
                ProjectFolder.CUSTOM_LABELS + File.separator
                + ProjectFile.CUSTOM_LABELS);
        if (customLabelsFile.exists()) {
            LOG.info(customLabelsFile.getAbsoluteFile() + " found");
        } else {
            LOG.fatal(ProjectFile.CUSTOM_LABELS + " not exist! Path ["
                    + customLabelsFile.getAbsolutePath() + "]");
            return;
        }
        LOG.info("-----------------------------------------------------------");
        LOG.info("            " + ProjectFile.CUSTOM_LABELS + " changes");
        LOG.info("-----------------------------------------------------------");
        CustomLabels customLabels = extractCustomLabels(customLabelsFile);
        customLabels.getLabels().forEach((cl) -> {
            String lang = cl.getLanguage();
            String tkey = cl.getFullName();
            if (!(lang == null || tkey == null || !langMap.containsKey(lang))) {
                Map<String, String> tMap = langMap.get(lang);
                if (tMap.containsKey(tkey)) {
                    String oldVal = cl.getValue() == null ? "" : cl.getValue();
                    String newVal = tMap.get(tkey);
                    LOG.debug("key [" + tkey + "], old value [" + oldVal
                            + "], new value [" + newVal + "]");
                    if (!oldVal.equals(newVal)) {
                        cl.setValue(newVal);
                        LOG.info("key [" + tkey + "], old value [" + oldVal
                                + "], new value [" + newVal + "]");
                    }
                }
            }
        });
        LOG.info("-----------------------------------------------------------");
        JAXBContext jaxbContext = JAXBContext.newInstance(CustomLabels.class);
	Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(customLabels, customLabelsFile);
        LOG.info(ProjectFile.CUSTOM_LABELS + " saved...");
    }
}
