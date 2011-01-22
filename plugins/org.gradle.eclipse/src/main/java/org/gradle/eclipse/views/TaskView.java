/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.eclipse.views;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.gradle.eclipse.GradleExecScheduler;
import org.gradle.foundation.ProjectView;

/**
 * @author rene@breskeby.com
 * */
public class TaskView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.gradle.eclipse.views.TaskView";

	private FormToolkit toolkit;
	private Form form;

	private GradleFileSelectionListener gradleFileSelectionListener;

	private TreeViewer taskviewer;

	/**
	 * The constructor.
	 */
	public TaskView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(final Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		form.setText("Available Gradle Tasks");
		toolkit.decorateFormHeading(form);
		form.getBody().setLayout(new FillLayout(SWT.VERTICAL));

		final Tree createTree = toolkit.createTree(form.getBody(), SWT.V_SCROLL);
		taskviewer = new TreeViewer(createTree);
		taskviewer.setLabelProvider(new WorkbenchLabelProvider());
		taskviewer.setContentProvider(new BaseWorkbenchContentProvider());
		toolkit.adapt(taskviewer.getControl(), false, false);

		gradleFileSelectionListener = new GradleFileSelectionListener(this);
		final ISelectionService selectionService = getSite().getWorkbenchWindow().getSelectionService();
		selectionService.addSelectionListener(gradleFileSelectionListener);
	}

	/**
	 * Passing the focus request to the form.
	 */
	public void setFocus() {
		form.setFocus();
	}

	/**
	 * Disposes the toolkit
	 */
	public void dispose() {
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(gradleFileSelectionListener);
		toolkit.dispose();
		super.dispose();
	}

	public void setModel(final IFile buildFile) {
		final List<ProjectView> projectViews = GradleExecScheduler.getInstance().getProjectViews(
				buildFile.getLocationURI().getPath());
		taskviewer.setInput(projectViews.get(0));
		taskviewer.refresh();
		form.redraw();
	}
}