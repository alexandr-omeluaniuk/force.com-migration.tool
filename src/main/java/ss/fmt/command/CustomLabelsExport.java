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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import ss.fmt.constants.ProjectFolder;
import ss.fmt.jaxb.model.CustomLabels;
import ss.lana.api.CommandArgument;
import ss.lana.api.CommandExecutor;

/**
 * Export custom labels translations.
 * @author ss
 */
@Component
class CustomLabelsExport implements CommandExecutor {
    /** Logger. */
    private static final Logger LOG = Logger
            .getLogger(CustomLabelsExport.class);
// ============================= ARGS =========================================
    /** Salesforce project 'src' folder path. */
    private static final String ARG_PROJECT_PATH = "project-src";
    /** Exported languages. */
    private static final String ARG_LANGUAGES = "languages";
// ============================================================================
    /** Custom labels file name. */
    private static final String C_CUSTOM_LABELS_FILE = "CustomLabels.labels";
    /** Translation file formatter. */
    private static final String C_TRANSLATION_FILE_FMT = "%s.translation";
// ============================================================================
    @Override
    public String name() {
        return "custom-labels-export";
    }
    @Override
    public String description() {
        return "export custom labels to external format";
    }
    @Override
    public void execute(List<CommandArgument> args) throws Exception {
        final Map<String, String> values = new HashMap<>();
        args.stream().forEach((arg) -> {
            values.put(arg.getName(), arg.getValue());
        });
        String projectPath = values.get(ARG_PROJECT_PATH);
        String languages = values.get(ARG_LANGUAGES);
        LOG.info("project absolute path [" + projectPath + "]");
        LOG.info("export languages [" + languages + "]");
        File customLabelsFile = new File(projectPath + File.separator
                + ProjectFolder.CUSTOM_LABELS + File.separator
                + C_CUSTOM_LABELS_FILE);
        if (customLabelsFile.exists()) {
            LOG.info(customLabelsFile.getAbsoluteFile() + " found");
        } else {
            LOG.fatal(C_CUSTOM_LABELS_FILE + " not exist! Path ["
                    + customLabelsFile.getAbsolutePath() + "]");
        }
        Map<String, File> translationFiles = new HashMap<>();
        for (String lang : languages.split(",")) {
            File f = new File(projectPath + File.separator
                    + ProjectFolder.TRANSLATIONS + File.separator
                    + String.format(C_TRANSLATION_FILE_FMT, lang.trim()));
            if (f.exists()) {
                LOG.info("translations for '" + lang.trim() + "' found. Path ["
                        + f.getAbsolutePath() + "]");
            } else {
                LOG.warn("translations for '" + lang.trim() + "' not found. "
                        + "Path [" + f.getAbsolutePath() + "]");
            }
        }
        CustomLabels customLabels = extractCustomLabels(customLabelsFile);
    }
    @Override
    public Set<CommandArgument> arguments() {
        Set<CommandArgument> args = new HashSet<>();
        args.add(new CommandArgument(ARG_PROJECT_PATH,
                "path to project 'src' folder", true));
        args.add(new CommandArgument(ARG_LANGUAGES,
                "list of the languages separated by comma, "
                        + "example: 'en_US, de'", true));
        return args;
    }
// ====================== PRIVATE =============================================
    /**
     * Extract custom labels.
     * @param file custom labels file.
     * @return unmarshalled object.
     * @throws Exception error.
     */
    private CustomLabels extractCustomLabels(final File file) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(CustomLabels.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        CustomLabels object = (CustomLabels) unmarshaller.unmarshal(file);
        LOG.info("total custom labels found [" + object.getLabels().size()
                + "]");
        return object;
    }
}
