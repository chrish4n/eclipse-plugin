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
package org.gradle.eclipse;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.gradle.eclipse.job.ConfigurationBasedBuildJob;
import org.gradle.eclipse.job.RefreshTaskJob;
import org.gradle.eclipse.job.UpdateClasspathJob;
import org.gradle.eclipse.launchConfigurations.GradleProcess;
import org.gradle.foundation.ProjectView;
import org.gradle.gradleplugin.foundation.GradlePluginLord;


/**
 * @author Rene Groeschke
 * */
public class GradleExecScheduler {

	private static GradleExecScheduler instance = null;

	/**
	 * The Gradle Scheduler manages the lifecycle of the buildinformation cache
	 * */
	private BuildInformationCache cache;
	
	
	public static GradleExecScheduler getInstance() {
		if(instance==null){
			instance = new GradleExecScheduler();
		}
		return instance;
	}
	
	private GradleExecScheduler(){
		this.cache = new BuildInformationCache();
	}

	// these both methods should be moved to BuildInformationCache
	public List<ProjectView> getProjectViews(IFile buildFile){
		String absolutePath = new File(buildFile.getFullPath().toString()).getAbsolutePath();	
		return getProjectViews(absolutePath);
	}
	
	public List<ProjectView> getProjectViews(String absolutePath) {
		if(cache.get(absolutePath)==null){
			refreshTaskView(absolutePath, true);
		}
		return cache.get(absolutePath);
	}

	public void refreshTaskView(final String absolutePath, boolean synched) {
		if(absolutePath!=null && !absolutePath.isEmpty()){
			final File absoluteDirectory = new File(absolutePath).getParentFile();
			
			if(absoluteDirectory.exists()){
				//run gradle only if directory exists
				final GradlePluginLord gradlePluginLord = new GradlePluginLord();
				gradlePluginLord.setGradleHomeDirectory(new File(GradlePlugin.getPlugin().getGradleHome()));
				gradlePluginLord.setCurrentDirectory(absoluteDirectory);
				RefreshTaskJob job = new RefreshTaskJob(gradlePluginLord, absolutePath, cache);
				
				if(!synched){
					job.setUser(false);
					job.setPriority(Job.LONG);
					job.schedule(); // start as soon as possible
				}
				else{
					// something wrong while calculating tasks
					final IStatus clcTsksStatus = job.calculateTasks(null);
					if(!clcTsksStatus.isOK()){
						final Display display = Display.getCurrent();
						display.asyncExec(new Runnable() {
					    		public void run() {
					    			MessageDialog.openError(display.getActiveShell(), "Error while calculating gradle tasks", clcTsksStatus.getMessage());					    		}
					    });
					}
				}			
			}
		}
	}
	
	public void updateProjectClasspath(String absoluteBuildPath, IProject projectToUpdate) throws CoreException{
		updateProjectClasspath(GradlePlugin.getDefault().getPreferenceStore(), absoluteBuildPath, projectToUpdate);
	}
	
	public void updateProjectClasspath(IPreferenceStore store, String absoluteBuildPath, IProject projectToUpdate) throws CoreException{
		final GradlePluginLord gradlePluginLord = new GradlePluginLord();
		gradlePluginLord.setGradleHomeDirectory(new File(GradlePlugin.getPlugin().getGradleHome(store)));

		File buildFile = new File(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(absoluteBuildPath));
		if(buildFile==null || !buildFile.exists()){
			throw(new CoreException(new Status(IStatus.ERROR, IGradleConstants.PLUGIN_ID, 
											   "buildPath: [ " + absoluteBuildPath + "] cannot be resolved")
				 ));
		}
			
		// create gradle build job
		Job job = new UpdateClasspathJob(projectToUpdate, gradlePluginLord, absoluteBuildPath);
		job.setUser(true);
		job.setPriority(Job.LONG);
		job.schedule(); // start as soon as possible
	}
	
	public void startGradleBuildRun(final ILaunchConfiguration configuration, final StringBuffer commandLine, final GradleProcess gradleProcess) throws CoreException{
		final GradlePluginLord gradlePluginLord = new GradlePluginLord();
		gradlePluginLord.setGradleHomeDirectory(new File(GradlePlugin.getPlugin().getGradleHome()));
		String buildfilePath = configuration.getAttribute(IGradleConstants.ATTR_LOCATION, "");
		File buildFile = new File(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(buildfilePath));
		if(buildFile==null || !buildFile.exists()){
			throw(new CoreException(new Status(IStatus.ERROR, 
											   IGradleConstants.PLUGIN_ID, 
											   "buildPath: [ " + buildfilePath + "] cannot be resolved")
				));
		}

		//buildfile could have any custom name so use -b flag
		commandLine.append(" -b ").append(buildFile.getName());
		gradlePluginLord.setCurrentDirectory(buildFile.getParentFile());
		
		
		// create gradle build job
		Job job = new ConfigurationBasedBuildJob(gradlePluginLord, gradleProcess, configuration, commandLine.toString());
		job.setUser(true);
		job.setPriority(Job.LONG);
		job.schedule(); // start as soon as possible
	}
}
