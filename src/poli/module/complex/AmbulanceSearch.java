package poli.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class AmbulanceSearch extends Search {
  
  private PathPlanning         pathPlanning;
  private Clustering           clustering;
  
  private EntityID             result;
  private Collection<EntityID> unsearchedBuildingIDs;
  
  
  public AmbulanceSearch( AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData ) {
    super( ai, wi, si, moduleManager, developData );
    
    this.unsearchedBuildingIDs = new HashSet<>();
    
    switch ( si.getMode() ) {
      case PRECOMPUTATION_PHASE:
        this.pathPlanning = moduleManager.getModule(
            "PoliAmbulance.Search.PathPlanning",
            "poli.module.algorithm.PathPlanning" );
        this.clustering = moduleManager.getModule(
            "PoliAmbulance.Search.Clustering", "poli.module.algorithm.KMeans" );
        break;
      case PRECOMPUTED:
        this.pathPlanning = moduleManager.getModule(
            "PoliAmbulance.Search.PathPlanning",
            "poli.module.algorithm.PathPlanning" );
        this.clustering = moduleManager.getModule(
            "PoliAmbulance.Search.Clustering", "poli.module.algorithm.KMeans" );
        break;
      case NON_PRECOMPUTE:
        this.pathPlanning = moduleManager.getModule(
            "PoliAmbulance.Search.PathPlanning",
            "poli.module.algorithm.PathPlanning" );
        this.clustering = moduleManager.getModule(
            "PoliAmbulance.Search.Clustering", "poli.module.algorithm.KMeans" );
        break;
    }
    
    registerModule( this.pathPlanning );
    registerModule( this.clustering );
  }
  
  
  @Override
  public Search updateInfo( MessageManager messageManager ) {
    super.updateInfo( messageManager );
    if ( this.getCountUpdateInfo() >= 2 ) {
      return this;
    }
    
    this.unsearchedBuildingIDs
        .removeAll( this.worldInfo.getChanged().getChangedEntities() );
    
    if ( this.unsearchedBuildingIDs.isEmpty() ) {
      this.reset();
      this.unsearchedBuildingIDs
          .removeAll( this.worldInfo.getChanged().getChangedEntities() );
    }
    return this;
  }
  
  
  @Override
  public Search calc() {
    this.result = null;
    this.pathPlanning.setFrom( this.agentInfo.getPosition() );
    this.pathPlanning.setDestination( this.unsearchedBuildingIDs );
    List<EntityID> path = this.pathPlanning.calc().getResult();
    if ( path != null && path.size() > 0 ) {
      this.result = path.get( path.size() - 1 );
    }
    return this;
  }
  
  
  private void reset() {
    this.unsearchedBuildingIDs.clear();
    
    Collection<StandardEntity> clusterEntities = null;
    if ( this.clustering != null ) {
      int clusterIndex = this.clustering
          .getClusterIndex( this.agentInfo.getID() );
      clusterEntities = this.clustering.getClusterEntities( clusterIndex );
      
    }
    if ( clusterEntities != null && clusterEntities.size() > 0 ) {
      for ( StandardEntity entity : clusterEntities ) {
        if ( entity instanceof Building && entity.getStandardURN() != REFUGE ) {
          this.unsearchedBuildingIDs.add( entity.getID() );
        }
      }
    } else {
      this.unsearchedBuildingIDs
          .addAll( this.worldInfo.getEntityIDsOfType( BUILDING, GAS_STATION,
              AMBULANCE_CENTRE, FIRE_STATION, POLICE_OFFICE ) );
    }
  }
  
  
  @Override
  public EntityID getTarget() {
    return this.result;
  }
  
  
  @Override
  public Search precompute( PrecomputeData precomputeData ) {
    super.precompute( precomputeData );
    if ( this.getCountPrecompute() >= 2 ) {
      return this;
    }
    return this;
  }
  
  
  @Override
  public Search resume( PrecomputeData precomputeData ) {
    super.resume( precomputeData );
    if ( this.getCountResume() >= 2 ) {
      return this;
    }
    this.worldInfo.requestRollback();
    return this;
  }
  
  
  @Override
  public Search preparate() {
    super.preparate();
    if ( this.getCountPreparate() >= 2 ) {
      return this;
    }
    this.worldInfo.requestRollback();
    return this;
  }
}