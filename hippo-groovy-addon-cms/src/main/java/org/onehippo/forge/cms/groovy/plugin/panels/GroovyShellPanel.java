/**
 * Copyright (C) 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.cms.groovy.plugin.panels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;
import org.onehippo.forge.cms.groovy.plugin.ShellOutput;
import org.onehippo.forge.cms.groovy.plugin.codemirror.CodeMirrorEditor;
import org.onehippo.forge.cms.groovy.plugin.domain.Script;
import org.onehippo.forge.cms.groovy.plugin.provider.GroovyScriptsDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;

/**
 * Panel for executing Groovy scripts
 *
 * @author Jeroen Reijn
 */
public class GroovyShellPanel extends PanelPluginBreadCrumbPanel {

    private final static Logger logger = LoggerFactory.getLogger(GroovyShellPanel.class);

    private final TextArea textArea;
    private final FileUploadField fileUpload;
    private ShellOutput output = new ShellOutput();
    private Script selectedScript = null;
    private String language = null;

    public GroovyShellPanel(final String componentId, final IBreadCrumbModel breadCrumbModel) {
        super(componentId, breadCrumbModel);

        final CompoundPropertyModel<ShellOutput> compoundPropertyModel = new CompoundPropertyModel<ShellOutput>(output);

        final Label shellFeedback = new Label("output", compoundPropertyModel);
        shellFeedback.setOutputMarkupId(true);

        GroovyScriptsDataProvider groovyScriptsDataProvider = new GroovyScriptsDataProvider();

        final Form form = new Form("shellform");

        form.add(new AjaxButton("ajax-upload", form) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final FileUpload uploadedFile = fileUpload.getFileUpload();
                if (uploadedFile != null) {
                    try {
                        String uploadedScript = IOUtils.toString(uploadedFile.getInputStream());
                        if (!StringUtils.isEmpty(uploadedScript)) {
                            GroovyShellPanel.this.setScript(uploadedScript);
                        }
                    } catch (IOException e) {
                        logger.warn("An exception occurred while trying to parse the uploaded script: {}", e);
                    }
                    target.addComponent(form);
                }
            }
        });

        final List<Script> scripts = getAvailableGroovyScriptsFromStore(groovyScriptsDataProvider);

        addAvailableScriptsInDropdownToForm(form, scripts);

        addAvailableLanguagesInDropdownToForm(form);

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("ajax-button", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> currentForm) {

                String scriptAsString = GroovyShellPanel.this.getScript();

                String language = GroovyShellPanel.this.getLanguage();
                
                Script script = new Script("boh");
                script.setLanguage(GroovyShellPanel.this.getLanguage());

                script.setScript(scriptAsString);
                script.setShellOutput(output);
                target.addComponent(shellFeedback);

                script.eval();
            }
        });

        form.setMultiPart(true);
        fileUpload = new FileUploadField("fileUpload");
        form.add(fileUpload);

        form.setOutputMarkupId(true);
        output.printVersion();
        textArea = new CodeMirrorEditor("script", new Model(""));
        textArea.setOutputMarkupId(true);
        shellFeedback.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
        form.add(shellFeedback);
        form.add(textArea);
        add(form);
    }

    private List<Script> getAvailableGroovyScriptsFromStore(final GroovyScriptsDataProvider groovyScriptsDataProvider) {
        final List<Script> scripts = new ArrayList<Script>();
        Iterator iterator = groovyScriptsDataProvider.iterator(0, 10);
        while(iterator.hasNext()) {
            Script script = (Script) iterator.next();
            scripts.add(script);
        }
        return scripts;
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("groovy-engine-panel-title");
    }

    private void addAvailableLanguagesInDropdownToForm(final Form form) {

        List<ScriptEngineFactory> factories = new ScriptEngineManager().getEngineFactories();

        List<String> languages = new ArrayList<String>();

        for (ScriptEngineFactory factory : factories) {
            languages.add(factory.getLanguageName());
        }

        final DropDownChoice<String> languageDropdownChoice =
                new DropDownChoice<String>("language",new PropertyModel<String>(this, "language"), languages);

        languageDropdownChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        languageDropdownChoice.setOutputMarkupId(true);
        form.add(languageDropdownChoice);
    }

    private void addAvailableScriptsInDropdownToForm(final Form form, final List<Script> scripts) {
        final DropDownChoice<Script> scriptDropDownChoice = new DropDownChoice<Script>("scripts",
                new PropertyModel<Script>(this, "selectedScript") , scripts, new IChoiceRenderer() {

            @Override
            public Object getDisplayValue(final Object object) {
                Script script = (Script) object;
                return script.getName();
            }

            @Override
            public String getIdValue(final Object object, final int i) {
                Script script = (Script) object;
                return script.getPath();
            }
        }) {
        };

        scriptDropDownChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {

            }
        });

        AjaxButton loadScript = new AjaxButton("load-script", form) {

            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form<?> form) {
                Script script = scriptDropDownChoice.getModelObject();
                if(script != null) {
                    GroovyShellPanel.this.setScript(script.getScript());
                }
                ajaxRequestTarget.addComponent(form);
            }
        };
        form.add(loadScript);

        scriptDropDownChoice.setOutputMarkupId(true);
        form.add(scriptDropDownChoice);
        if(scripts.size() == 0) {
            scriptDropDownChoice.setVisible(false);
            loadScript.setVisible(false);
        }
    }

    public String getScript() {
        return (String) textArea.getModelObject();
    }

    public void setScript(String newScript) {
        textArea.setModelObject(newScript);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
