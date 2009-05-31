package org.apache.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role=ProjectDependenciesResolver.class)
public class DefaultProjectDependenciesResolver
    implements ProjectDependenciesResolver
{
    @Requirement
    private RepositorySystem repositorySystem;
    
    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;
    
    public Set<Artifact> resolve( MavenProject project, String scope, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {        
        List<String> exclusions = new ArrayList<String>();
        
        for ( Dependency d : project.getDependencies() )
        {
            if ( d.getExclusions() != null )
            {
                for ( Exclusion e : d.getExclusions() )
                {
                    exclusions.add(  e.getGroupId() + ":" + e.getArtifactId() );
                }
            }
        }
        
        ArtifactFilter scopeFilter = new ScopeArtifactFilter( scope );
        
        ArtifactFilter filter; 

        if ( exclusions != null )
        {
            filter = new AndArtifactFilter( Arrays.asList( new ArtifactFilter[]{ new ExcludesArtifactFilter( exclusions ), scopeFilter } ) );
        }
        else
        {
            filter = scopeFilter;
        }
                
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( new ProjectArtifact( project ) )
            .setResolveRoot( false )
            .setResolveTransitively( true )
            .setLocalRepository( localRepository )
            .setRemoteRepostories( remoteRepositories )
            .setManagedVersionMap( project.getManagedVersionMap() )
            .setFilter( filter );
    
        ArtifactResolutionResult result = repositorySystem.resolve( request );                
        resolutionErrorHandler.throwErrors( request, result );
        project.setArtifacts( result.getArtifacts() );
        return result.getArtifacts();        
    }  
}