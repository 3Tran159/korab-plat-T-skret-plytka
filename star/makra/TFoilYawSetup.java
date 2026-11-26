package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;import star.meshing.*;import star.flow.*;import star.material.*;
import star.segregatedflow.*;import star.turbulence.*;import star.kwturb.*;import star.vof.*;import star.mixturemultiphase.*;import star.metrics.*;
import star.motion.*;import star.trimmer.*;import star.prismmesher.*;import star.resurfacer.*;import star.vis.*;import star.post.*;
import java.nio.file.*;import java.io.*;import java.util.*;import java.lang.reflect.*;

/**
 * TFoilYawSetup - plat T KORAB, PELNY model (bez symetrii), przedni strut skrecony o yaw_deg wokol osi pionowej,
 * woda ze swobodna powierzchnia (VOF), turbulencja SST k-omega IDDES (DES: RANS przy scianie, LES w oderwaniu).
 * Wszystko natywnie w drzewie (GUI): uklady "TF7 obrot - pochylenie" / "TF7 obrot - skret", bloki siatki, fizyka,
 * raporty TF7_*, wykresy "TF7 ...", sceny "TF7 0x ..." z zapisem animacji, historia rozwiazania TF7_historia.simh.
 */
public class TFoilYawSetup extends StarMacro {
 Simulation s; Path out; Properties cfg=new Properties(); Units m; Map<String,Object> log=new LinkedHashMap<>();
 double B=0.1, XQC=0.1709, ZQC=-0.0164, RHO=997.561, MU=8.8871e-4, S_FULL=0.051916;
 double U,ZS,XS;
 double d(String k,double v){return Double.parseDouble(cfg.getProperty(k,""+v));}
 int i(String k,int v){return Integer.parseInt(cfg.getProperty(k,""+v));}
 static String q(String x){return "\""+x.replace("\\","/").replace("\"","'")+"\"";}
 String json(Object a){
  if(a==null)return "null";
  if(a instanceof Number){double x=((Number)a).doubleValue();return Double.isFinite(x)?a.toString():"null";}
  if(a instanceof Boolean)return a.toString();
  if(a instanceof Map<?,?> mm){StringBuilder b=new StringBuilder("{");boolean f=true;for(Map.Entry<?,?> e:mm.entrySet()){if(!f)b.append(",");f=false;b.append(q(String.valueOf(e.getKey()))).append(":").append(json(e.getValue()));}return b.append("}").toString();}
  if(a instanceof Collection<?> c){StringBuilder b=new StringBuilder("[");boolean f=true;for(Object o:c){if(!f)b.append(",");f=false;b.append(json(o));}return b.append("]").toString();}
  return q(String.valueOf(a));
 }
 void write(String n,Object v){try{Path p=out.resolve(n),t=out.resolve(n+".tmp");Files.writeString(t,json(v));Files.move(t,p,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){}}
 FieldFunction ff(String n){FieldFunction f=s.getFieldFunctionManager().getFunction(n);if(f==null)throw new IllegalStateException("Missing field "+n);return f;}
 FieldFunction ffPres(String pres){for(FieldFunction f:s.getFieldFunctionManager().getObjects())if(f.getPresentationName().equals(pres))return f;return null;}
 FieldFunction wf(VofWave w,String prefix){FieldFunction f=ffPres(prefix+w.getPresentationName());if(f==null)throw new IllegalStateException("Missing wave field "+prefix+w.getPresentationName());return f;}
 void scalar(ScalarProfile p,double v){p.setMethod(ConstantScalarProfileMethod.class);p.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(v);}
 void sf(ScalarProfile p,FieldFunction f){p.setMethod(FunctionScalarProfileMethod.class);p.getMethod(FunctionScalarProfileMethod.class).setFieldFunction(f);}
 void vf(VectorProfile p,FieldFunction f){p.setMethod(FunctionVectorProfileMethod.class);p.getMethod(FunctionVectorProfileMethod.class).setFieldFunction(f);}
 void frac(VolumeFractionProfile p,FieldFunction f){p.setMethod(CompositeNMinus1ArrayProfileMethod.class);sf(p.getMethod(CompositeNMinus1ArrayProfileMethod.class).getProfile(0),f);}
 // wywolanie przez refleksje (API rozni sie miedzy wersjami) - blad nie przerywa makra, trafia do logu
 Object call(Object o,String name,Object... a){
  for(Method mt:o.getClass().getMethods()){
   if(!mt.getName().equals(name)||mt.getParameterCount()!=a.length)continue;
   try{return mt.invoke(o,a);}catch(Throwable e){}
  }
  throw new IllegalStateException("call failed: "+o.getClass().getSimpleName()+"."+name);
 }
 boolean hasMethod(Object o,String n){for(Method mt:o.getClass().getMethods())if(mt.getName().equals(n))return true;return false;}
 List<String> warn=new ArrayList<>();
 void soft(String what,Runnable r){try{r.run();}catch(Throwable e){warn.add(what+": "+e);}}

 public void execute(){
  s=getActiveSimulation();
  out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR",System.getProperty("user.dir")));
  String stage="init";
  try{
   try(Reader r=Files.newBufferedReader(out.resolve("control.properties"))){cfg.load(r);}
   m=s.getUnitsManager().getObject("m");
   U=d("U",8.333); double pitch=d("rot_deg",7.42), yaw=d("yaw_deg",7.0), h=d("h_m",0.07519); XS=d("yaw_axis_x",0.16);
   ZS=ZQC+h; log.put("U",U);log.put("pitch_rot_deg",pitch);log.put("yaw_deg",yaw);log.put("h_m",h);log.put("z_surface_m",ZS);
   Region region=s.getRegionManager().getRegion("Subtract");
   s.getSimulationIterator().getAutoSave().setEnabled(false);
   if(!cfg.getProperty("ramp_only","false").equals("true"))s.getSolution().clearSolution(Solution.Clear.Fields,Solution.Clear.History);
   ((UserFieldFunction)ffPres("Velocity 2")).setDefinition("[-"+U+",0,0]");
   PhysicsContinuum p1=(PhysicsContinuum)s.getContinuumManager().getContinuum("Physics 1");
   for(Object mo:new ArrayList<>(p1.getModelManager().getObjects()))if(mo.getClass().getSimpleName().contains("GammaTransition"))p1.disableModel((Model)mo);

   if(cfg.getProperty("scenes_only","false").equals("true")){        // tylko przebudowa scen na gotowym *_SETUP.sim
    stage="scenes_only";
    for(Scene sc:new ArrayList<>(s.getSceneManager().getScenes()))if(sc.getPresentationName().startsWith("TF7")){final Scene x=sc;soft("del "+x.getPresentationName(),()->{try{call(s.getSceneManager(),"deleteScenes",new NeoObjectVector(new Object[]{x}));}catch(Throwable e){call(s.getSceneManager(),"remove",x);}});}
    cpF=ffPres("TF7_Cp");FvRepresentation fv=(FvRepresentation)s.getRepresentationManager().getObject("Volume Mesh");
    scenes(region,region.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace1"),region.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace2"),fv);
    log.put("warnings",warn);s.saveState(out.resolve(cfg.getProperty("name","TF7")+"_SETUP.sim").toString());log.put("stage","done");write("SCENES.json",log);return;
   }
   if(cfg.getProperty("ramp_only","false").equals("true")){          // rampa skretu: obrot kierunku naplywu w czasie na gotowym rozwiazaniu
    stage="ramp";ramp(region);
    for(Scene sc:new ArrayList<>(s.getSceneManager().getScenes()))if(sc.getPresentationName().startsWith("TF7")){final Scene x=sc;soft("del "+x.getPresentationName(),()->{try{call(s.getSceneManager(),"deleteScenes",new NeoObjectVector(new Object[]{x}));}catch(Throwable e){call(s.getSceneManager(),"remove",x);}});}
    for(Object o:new ArrayList<>(s.getPartManager().getObjects()))if(((NamedObject)o).getPresentationName().startsWith("TF7")){final Object x=o;soft("del part",()->call(s.getPartManager(),"removeObjects",new NeoObjectVector(new Object[]{x})));}
    cpF=ffPres("TF7_Cp");FvRepresentation fv=(FvRepresentation)s.getRepresentationManager().getObject("Volume Mesh");
    Boundary fo=region.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace1"),stb=region.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace2");
    stage="ramp_scenes";scenes(region,fo,stb,fv);
    stage="ramp_history";for(Object o:new ArrayList<>(s.get(SolutionHistoryManager.class).getObjects())){final Object x=o;soft("del hist",()->call(s.get(SolutionHistoryManager.class),"removeObjects",new NeoObjectVector(new Object[]{x})));}
    history(region);
    for(SolverStoppingCriterion c:s.getSolverStoppingCriterionManager().getObjects())if(c instanceof PhysicalTimeStoppingCriterion)((PhysicalTimeStoppingCriterion)c).getMaximumTime().setValue(d("t_end_s",5.0));
    log.put("warnings",warn);s.saveState(out.resolve(cfg.getProperty("name","TF7")+"_SETUP.sim").toString());log.put("stage","done");write("SETUP.json",log);return;
   }
   stage="full_domain";
   SimulationPartManager pm=s.get(SimulationPartManager.class);
   block(pm,"Block",new double[]{-4,-2,-2},new double[]{3,2,0.4});
   block(pm,"Block 3",new double[]{-0.2,-0.045,-0.03},new double[]{0.25,0.045,ZS+0.10});          // strut + zlacze 3,125 mm
   block(pm,"Block 3 2",new double[]{-0.35,-0.45,-0.07},new double[]{0.35,0.45,0.043});         // otoczenie plata 6,25 mm
   block(pm,"Block 3 2 2",new double[]{-0.32,-0.12,-0.12},new double[]{0.35,0.12,ZS+0.15});     // otoczenie strutu 6,25 mm

   stage="rotation";
   GeometryPart cad=null;for(GeometryPart g:pm.getLeafParts())if(g.getPresentationName().equals("poprawione_plat_olek"))cad=g;
   if(cad==null)throw new IllegalStateException("CAD part not found");
   CoordinateSystem lab=s.getCoordinateSystemManager().getLabCoordinateSystem();
   CartesianCoordinateSystem cp=lab.getLocalCoordinateSystemManager().createLocalCoordinateSystem(CartesianCoordinateSystem.class,"Cartesian");
   cp.setPresentationName("TF7 obrot - pochylenie (os y, 1/4 cieciwy nasady)");cp.getOrigin().setCoordinate(m,m,m,new DoubleVector(new double[]{XQC,0,ZQC}));
   CartesianCoordinateSystem cy=lab.getLocalCoordinateSystemManager().createLocalCoordinateSystem(CartesianCoordinateSystem.class,"Cartesian");
   cy.setPresentationName("TF7 obrot - skret strutu (os z, 1/4 cieciwy strutu)");cy.getOrigin().setCoordinate(m,m,m,new DoubleVector(new double[]{XS,0,0}));
   log.put("rot_pitch",rotate(cad,new double[]{0,1,0},Math.toRadians(pitch),cp));
   log.put("rot_yaw",rotate(cad,new double[]{0,0,1},Math.toRadians(yaw),cy));
   cad.setPresentationName("poprawione_plat_olek (pochylenie "+pitch+" deg, skret "+yaw+" deg)");

   stage="mesh_controls";
   AutoMeshOperation am=(AutoMeshOperation)s.get(MeshOperationManager.class).getObject("Automated Mesh 2");
   coarsen(am,"Volumetric Control",2.0);                                                  // plat 1,56 -> 3,125 mm (jak kampania zanurzen)
   // 2026-11-21: model optymalizacyjny plytki - mesh_scale skaluje rozmiar bazowy (wszystkie kontrolki sa wzgledne do bazy)
   if(cfg.getProperty("mesh_scale")!=null){double ms=d("mesh_scale",1.0);Object bs=am.getDefaultValues().get(BaseSize.class);
    try{bs.getClass().getMethod("setValue",double.class).invoke(bs,B*ms);}catch(Throwable e){call(bs,"setDefinition",""+(B*ms));}
    log.put("mesh_scale",ms);log.put("base_size_m",B*ms);}
   volume(am,"TF7 pas powierzchni przy placie (12,5 x 12,5 x 3,1 mm)",new double[]{-0.9,-0.6,ZS-0.035},new double[]{0.5,0.6,ZS+0.05},0.0126,0.0032);
   volume(am,"TF7 pas powierzchni cala domena (50 x 50 x 12,5 mm)",new double[]{-4.1,-2.1,ZS-0.08},new double[]{3.1,2.1,ZS+0.12},0.051,0.0126);
   // 2026-11-19: strut nad Block 3 2 2 (z > ZS+0,15) siedzial w komorkach bazowych 0,1 m przy scianach 0,1 mm -> drzazgi 1e-14 m3 na krawedzi (x=0,195, z=0,29..0,33)
   double sb=d("strut_top_cell_m",0.00625);
   volume(am,"TF7 strut nad lustrem do gory domeny ("+(sb*1000)+" mm)",new double[]{d("strut_box_x0",0.04),-d("strut_box_y",0.05),ZS+0.10},new double[]{d("strut_box_x1",0.26),d("strut_box_y",0.05),0.41},sb,sb);
   soft("strut target surface",()->{SurfaceCustomMeshControl sc=(SurfaceCustomMeshControl)am.getCustomMeshControls().getObject("Copy of płat+wspornik");
    if(sc==null)for(CustomMeshControl c:am.getCustomMeshControls().getObjects())if(c instanceof SurfaceCustomMeshControl&&c.getPresentationName().startsWith("Copy of p"))sc=(SurfaceCustomMeshControl)c;
    sc.getCustomConditions().get(PartsTargetSurfaceSizeOption.class).setSelected(PartsTargetSurfaceSizeOption.Type.CUSTOM);
    setRel(sc.getCustomValues().get(PartsTargetSurfaceSize.class).getRelativeSize(),100*d("strut_target_surf_m",0.003125)/B);
    log.put("strut_target_surface_m",d("strut_target_surf_m",0.003125));
    if(cfg.getProperty("strut_min_surf_m")!=null){setRel(sc.getCustomValues().get(PartsMinimumSurfaceSize.class).getRelativeSize(),100*d("strut_min_surf_m",1e-4)/B);
     log.put("strut_min_surface_m",d("strut_min_surf_m",1e-4));}});
   if(cfg.getProperty("plate_dz")!=null)plate(am,yaw,cy);
   if("false".equals(cfg.getProperty("project_to_cad")))soft("project_to_cad",()->{Object o=am.getDefaultValues().get(ProjectToCadOption.class);call(o,"setProjectToCad",false);log.put("project_to_cad",false);});
   write("SETUP.json",log);

   stage="mesh";
   long t0=System.nanoTime();s.get(MeshPipelineController.class).generateVolumeMesh();
   FvRepresentation fv=(FvRepresentation)s.getRepresentationManager().getObject("Volume Mesh");
   log.put("cells",fv.getCellCount());log.put("mesh_s",(System.nanoTime()-t0)/1e9);write("SETUP.json",log);
   if(fv.getCellCount()>Long.parseLong(cfg.getProperty("max_cells","3300000")))throw new IllegalStateException("za duzo komorek dla GPU 12 GB: "+fv.getCellCount());
   Boundary foil=region.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace1");
   Boundary strut=region.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace2");
   List<String> bn=new ArrayList<>();for(Boundary b:region.getBoundaryManager().getBoundaries())bn.add(b.getPresentationName()+" | "+b.getBoundaryType().getPresentationName());log.put("boundaries_before",bn);
   if(foil==null||strut==null)throw new IllegalStateException("walls not found "+bn);
   stage="mesh_qa";meshQA(region);
   if("true".equals(cfg.getProperty("mesh_only"))){s.saveState(out.resolve(cfg.getProperty("name","TF7")+"_MESH.sim").toString());log.put("stage","mesh_only_done");log.put("warnings",warn);write("SETUP.json",log);return;}

   stage="physics";vof(region);
   stage="reports";reports(region,foil,strut);
   if(plateB!=null){List<Boundary> P=List.of(plateB);
    force("TF7_D_plytka",P,new double[]{-1,0,0},"PRESSURE_AND_SHEAR");force("TF7_Fy_plytka",P,new double[]{0,1,0},"PRESSURE_AND_SHEAR");
    force("TF7_L_plytka",P,new double[]{0,0,1},"PRESSURE_AND_SHEAR");log.put("plate_reports","TF7_D_plytka, TF7_Fy_plytka, TF7_L_plytka [N]");}
   if(cfg.getProperty("ramp_to_deg")!=null){stage="ramp";ramp(region);}
   stage="scenes";scenes(region,foil,strut,fv);
   if(plateB!=null){int na=0;   // plytka na wszystkich wyswietlaczach, ktore pokazuja strut
    for(Scene sc:s.getSceneManager().getScenes())for(Object dp:sc.getDisplayerManager().getObjects())try{
     Object ip=call(dp,"getInputParts");Collection<?> cur=(Collection<?>)call(ip,"getObjects");
     if(cur.contains(strut)&&!cur.contains(plateB)){List<Object> nl=new ArrayList<>(cur);nl.add(plateB);call(ip,"setObjects",nl);na++;}}catch(Throwable e){}
    log.put("plate_in_displayers",na);}
   if(!"false".equals(cfg.getProperty("h_scenes")))
{stage="scenes_H";hScenes(region,foil,strut,fv);}
   stage="history";history(region);
   log.put("warnings",warn);
   String nm=cfg.getProperty("name","TF7");
   s.saveState(out.resolve(nm+"_SETUP.sim").toString());
   log.put("saved",nm+"_SETUP.sim");log.put("stage","done");write("SETUP.json",log);
  }catch(Throwable e){
   log.put("stage",stage);log.put("error",e.toString());log.put("warnings",warn);
   StringWriter sw=new StringWriter();e.printStackTrace(new PrintWriter(sw));log.put("trace",sw.toString().substring(0,Math.min(4000,sw.toString().length())));
   write("SETUP.json",log);throw new RuntimeException(e);
  }
 }
 // ---------- plytka antywentylacyjna na strucie: pozioma, plate_dz pod lustrem, plate_L (wzdluz cieciwy) x plate_W x plate_t ----------
 GeometryPart plateP; Boundary plateB;
 void plate(AutoMeshOperation am,double yaw,CoordinateSystem cy) throws Exception{
  double dz=d("plate_dz",0.02),L=d("plate_L",0.10),W=d("plate_W",0.10),t=d("plate_t",0.003),xle=d("plate_x_le",0.1955),cell=d("plate_cell_m",0.0015);
  double zc=ZS-dz;
  SimulationPartManager pm=s.get(SimulationPartManager.class);
  String stl=cfg.getProperty("plate_stl");
  if(stl!=null){   // oplywowa plytka z pliku STL (10_GEOMETRIA, generator 00_START\plytka_stl.py), wspolrzedne laboratorium [m]
   Set<GeometryPart> before=new HashSet<>(s.get(SimulationPartManager.class).getLeafParts());
   s.get(PartImportManager.class).importStlPart(stl,"OneSurfacePerPatch",m,true,d("plate_stl_tol",1.0E-9));
   for(GeometryPart g:s.get(SimulationPartManager.class).getLeafParts())if(!before.contains(g))plateP=g;
   if(plateP==null)throw new IllegalStateException("import STL plytki: brak nowej czesci");
   log.put("plate_stl",stl);
  }else{
   SimpleBlockPart bp=s.get(MeshPartFactory.class).createNewBlockPart(s.getGeometryPartManager());
   bp.setCorners(new DoubleVector(new double[]{xle-L,-W/2,zc-t/2}),new DoubleVector(new double[]{xle,W/2,zc+t/2}));bp.rebuildSimpleShapePart();plateP=bp;
  }
  plateP.setPresentationName("TF7 plytka antywentylacyjna");
  if(Math.abs(yaw)>1e-9)log.put("plate_rot_yaw",rotate(plateP,new double[]{0,0,1},Math.toRadians(yaw),cy));
  log.put("plate",String.format(Locale.ROOT,"x %.4f..%.4f, y +-%.3f, z %.4f (%.3f m pod lustrem), grubosc %.4f m",xle-L,xle,W/2,zc,dz,t));
  // odjecie od domeny (tak jak CAD platu)
  SubtractPartsOperation sub=(SubtractPartsOperation)s.get(MeshOperationManager.class).getObject("Subtract");
  sub.getInputGeometryObjects().add(plateP);sub.execute();
  PartSurface ps=null;for(GeometryPart gp:s.getGeometryPartManager().getParts())if(gp.getPresentationName().equals("Subtract"))
   for(PartSurface p:gp.getPartSurfaces())if(p.getPresentationName().contains("plytka"))ps=p;
  if(ps==null)throw new IllegalStateException("brak powierzchni plytki w Subtract");
  log.put("plate_surface",ps.getPresentationName());
  final PartSurface fps=ps;
  // wlasna granica (sciana) dla plytki - inaczej powierzchnia trafia do domyslnej granicy regionu
  Region rg=s.getRegionManager().getRegion("Subtract");
  for(Boundary b:rg.getBoundaryManager().getBoundaries())try{((Collection<?>)call(call(b,"getPartSurfaceGroup"),"getObjects")).remove(fps);}catch(Throwable e){}
  plateB=(Boundary)call(rg.getBoundaryManager(),"createEmptyBoundary");
  plateB.setPresentationName("TF7 plytka");
  try{call(call(plateB,"getPartSurfaceGroup"),"setObjects",List.of(fps));}catch(Throwable e){call(call(plateB,"getPartSurfaceGroup"),"add",fps);}
  plateB.setBoundaryType(WallBoundary.class);
  log.put("plate_boundary",plateB.getPresentationName()+" <- "+call(call(plateB,"getPartSurfaceGroup"),"getObjects"));
  SurfaceCustomMeshControl sc=am.getCustomMeshControls().createSurfaceControl();sc.setPresentationName("TF7 plytka - powierzchnia");sc.getGeometryObjects().setObjects(fps);
  sc.getCustomConditions().get(PartsMinimumSurfaceSizeOption.class).setSelected(PartsMinimumSurfaceSizeOption.Type.CUSTOM);
  sc.getCustomConditions().get(PartsTargetSurfaceSizeOption.class).setSelected(PartsTargetSurfaceSizeOption.Type.CUSTOM);
  setRel(sc.getCustomValues().get(PartsMinimumSurfaceSize.class).getRelativeSize(),100*d("plate_min_surf_m",0.0005)/B);
  setRel(sc.getCustomValues().get(PartsTargetSurfaceSize.class).getRelativeSize(),100*cell/B);
  soft("plate prisms off",()->{try{
    Object o0=null;for(Object c:sc.getCustomConditions().getObjects())if(c.getClass().getSimpleName().equals("PartsCustomizePrismMesh"))o0=c;
    Object o=o0.getClass().getMethod("getCustomPrismOptions").invoke(o0);
    Object sel=o.getClass().getMethod("getSelectedElement").invoke(o);Object dis=null;
    for(Object k:sel.getClass().getEnumConstants())if(k.toString().toUpperCase().contains("DISABLE"))dis=k;
    for(Method mt:o.getClass().getMethods())if(mt.getName().equals("setSelected")&&mt.getParameterCount()==1)try{mt.invoke(o,dis);log.put("plate_prisms","DISABLE");break;}catch(Throwable e){}
   }catch(Throwable e){throw new RuntimeException(e);}});
  double r=Math.hypot(Math.max(Math.abs(xle-XS),Math.abs(xle-L-XS)),W/2)+0.01;
  double vc=d("plate_vol_m",cell),hz=d("plate_zone_dz",0.008);
  volume(am,"TF7 plytka - strefa "+(vc*1000)+" mm",new double[]{XS-r,-r,zc-hz},new double[]{XS+r,r,zc+hz},vc,vc);
  write("SETUP.json",log);
 }
 // kontrola jakosci siatki (raporty zostaja w .sim, widoczne w GUI jako TF7_siatka_*)
 void meshQA(Region reg){
  Map<String,Object> q=new LinkedHashMap<>();List<Region> R=List.of(reg);
  String[][] st={{"CellQuality","min"},{"VolumeChange","min"},{"SkewnessAngle","max"},{"Volume","min"}};
  for(String[] x:st){try{FieldFunction f=s.getFieldFunctionManager().getFunction(x[0]);
    double v;if(x[1].equals("min")){MinReport r=s.getReportManager().createReport(MinReport.class);r.setPresentationName("TF7_siatka_"+x[0]+"_min");r.setFieldFunction(f);r.getParts().setObjects(R);v=r.getReportMonitorValue();}
    else{MaxReport r=s.getReportManager().createReport(MaxReport.class);r.setPresentationName("TF7_siatka_"+x[0]+"_max");r.setFieldFunction(f);r.getParts().setObjects(R);v=r.getReportMonitorValue();}
    q.put(x[0]+"_"+x[1],v);}catch(Throwable e){q.put(x[0],"? "+e);}}
  String[][] cnt={{"CellQuality","<","1e-3"},{"VolumeChange","<","1e-3"},{"SkewnessAngle",">","85"},{"Volume","<","1e-10"}};
  for(String[] x:cnt){try{UserFieldFunction u=s.getFieldFunctionManager().createFieldFunction();u.setPresentationName("TF7_siatka_zla_"+x[0]);u.setFunctionName("TF7_siatka_zla_"+x[0]);
    u.setDefinition("($"+x[0]+" "+x[1]+" "+x[2]+") ? 1 : 0");SumReport r=s.getReportManager().createReport(SumReport.class);r.setPresentationName("TF7_siatka_liczba_"+x[0]+x[1]+x[2]);
    r.setFieldFunction(u);r.getParts().setObjects(R);q.put("n "+x[0]+x[1]+x[2],r.getReportMonitorValue());}catch(Throwable e){q.put("n "+x[0],"? "+e);}}
  log.put("mesh_qa",q);write("SETUP.json",log);
 }
 void block(SimulationPartManager pm,String name,double[] lo,double[] hi){
  SimpleBlockPart b=null;for(GeometryPart g:pm.getLeafParts())if(g.getPresentationName().equals(name)&&g instanceof SimpleBlockPart)b=(SimpleBlockPart)g;
  if(b==null){warn.add("block "+name+" not found");return;}
  b.setCorners(new DoubleVector(lo),new DoubleVector(hi));b.rebuildSimpleShapePart();
  log.put("block "+name,Arrays.toString(lo)+" .. "+Arrays.toString(hi));
 }
 List<String> rotate(GeometryPart part,double[] axis,double rad,CoordinateSystem cs){
  List<String> tried=new ArrayList<>();SimulationPartManager pm=s.get(SimulationPartManager.class);
  for(Method mt:pm.getClass().getMethods()){
   if(!mt.getName().equals("rotateParts")||mt.getParameterCount()!=5)continue;
   Class<?>[] pt=mt.getParameterTypes();
   try{Object a0=Collection.class.isAssignableFrom(pt[0])?new ArrayList<>(List.of(part)):new NeoObjectVector(new Object[]{part});
    Object a2=Collection.class.isAssignableFrom(pt[2])?new ArrayList<>(List.of(m,m,m)):new NeoObjectVector(new Object[]{m,m,m});
    mt.invoke(pm,a0,new DoubleVector(axis),a2,rad,cs);tried.add("OK");return tried;}catch(Throwable e){tried.add("fail "+e.getCause());}
  }
  throw new IllegalStateException("rotateParts failed "+tried);
 }
 void setRel(Object o,double pct){for(String mth:new String[]{"setRelativeSize","setPercentage","setValue"})try{o.getClass().getMethod(mth,double.class).invoke(o,pct);return;}catch(Throwable e){}throw new IllegalStateException("cannot set relative size");}
 double getRel(Object o){for(String g:new String[]{"getPercentage","getValue"})try{return ((Number)o.getClass().getMethod(g).invoke(o)).doubleValue();}catch(Throwable e){}return Double.NaN;}
 void coarsen(AutoMeshOperation am,String ctrl,double k){
  VolumeCustomMeshControl c=(VolumeCustomMeshControl)am.getCustomMeshControls().getObject(ctrl);
  Object rel=c.getCustomValues().get(VolumeControlSize.class).getRelativeSize();double old=getRel(rel);setRel(rel,old*k);
  c.setPresentationName(ctrl+" (rozrzedzone x"+k+")");log.put("coarsen "+ctrl,old+" -> "+getRel(rel)+" % base");
 }
 void volume(AutoMeshOperation am,String name,double[] lo,double[] hi,double iso,double dz){
  SimpleBlockPart p=s.get(MeshPartFactory.class).createNewBlockPart(s.getGeometryPartManager());
  p.setPresentationName(name);p.setCorners(new DoubleVector(lo),new DoubleVector(hi));p.rebuildSimpleShapePart();
  VolumeCustomMeshControl c=am.getCustomMeshControls().createVolumeControl();c.setPresentationName(name);c.getGeometryObjects().setObjects(p);
  VolumeControlTrimmerSizeOption t=c.getCustomConditions().get(VolumeControlTrimmerSizeOption.class);
  t.setVolumeControlBaseSizeOption(true);c.getCustomValues().get(VolumeControlSize.class).setRelativeSize(100*iso/B);
  try{t.setTrimmerAnisotropicSizeOption(true);TrimmerAnisotropicSize ta=c.getCustomValues().get(TrimmerAnisotropicSize.class);
   try{ta.getRelativeOrAbsoluteOption().setSelected(RelativeOrAbsoluteOption.Type.RELATIVE);}catch(Throwable e){}
   ta.setZSize(true);setRel(ta.getRelativeZSize(),100*dz/B);}catch(Throwable e){warn.add("aniso "+name+": "+e);}
 }

 // ---------- rampa: kierunek naplywu (prad i wiatr fali plaskiej) obraca sie w czasie; boki domeny: wlot (-y) i wylot (+y) ----------
 void ramp(Region reg){
  double y0=d("yaw_deg",7.0),y1=d("ramp_to_deg",45.0),t0=d("ramp_t0_s",2.0),T=d("ramp_T_s",3.0),b=Math.toRadians(y1-y0);
  final double FT=d("ramp_fast_T_s",0),YF=d("ramp_fast_to_deg",y0);   // FT>0: najpierw szybko y0->YF w FT, potem YF->y1 w T
  if(FT>0)log.put("ramp_fast","skret "+y0+" -> "+YF+" deg w "+FT+" s (od t0), potem -> "+y1+" deg w "+T+" s");
  String beta="("+b+"*min(max(($Time-<"+t0+" s>)/<"+T+" s>,0),1))";
  String vec="[<"+(-U)+" m/s>*cos("+beta+"),<"+U+" m/s>*sin("+beta+"),<0 m/s>]";
  PhysicsContinuum pc=reg.getPhysicsContinuum();VofWaveModel vm=pc.getModelManager().getModel(VofWaveModel.class);
  FlatVofWave fw=null;for(Object w:vm.getVofWaveManager().getObjects())if(w instanceof FlatVofWave)fw=(FlatVofWave)w;
  if(fw==null)throw new IllegalStateException("no flat wave");flat=fw;
  if("inflow".equals(cfg.getProperty("ramp_mode","mesh"))){       // stary wariant (BLEDNY fizycznie: przyspiesza caly basen bokiem) - tylko na zyczenie
   fw.getCurrent().setDefinition(vec);fw.getWind().setDefinition(vec);log.put("ramp_velocity",vec);
  }else{
   // 2026-11-19: obrot CALEJ siatki (ruch bryly sztywnej) wokol osi strutu; naplyw staly (-U,0,0) w ukladzie laboratorium
   double w=b/T;
   RotatingMotion rm=s.get(MotionManager.class).createMotion(RotatingMotion.class,"Rotation");
   rm.setPresentationName("TF7 skret strutu - obrot siatki wokol osi strutu");
   Object ad=rm.getAxisDirection();
   try{call(ad,"setComponents",0.0,0.0,1.0);}catch(Throwable e){call(ad,"setCoordinate",m,m,m,new DoubleVector(new double[]{0,0,1}));}
   Object ao=rm.getAxisOrigin();
   try{call(ao,"setComponents",XS,0.0,0.0);}catch(Throwable e){call(ao,"setCoordinate",m,m,m,new DoubleVector(new double[]{XS,0,0}));}
   try{log.put("ramp_axis",String.valueOf(call(ad,"getValue"))+" @ "+String.valueOf(call(ao,"getValue")));}catch(Throwable e){}
   String om="($Time > <"+t0+" s> && $Time < <"+(t0+T)+" s>) ? <"+w+" radian/s> : <0 radian/s>";
   if(FT>0){   // 2026-11-22: rampa dwuetapowa: szybko y0 -> yF w FT, potem y1 w T (model optymalizacyjny: bez nagłego startu pod katem)
    double w1=Math.toRadians(YF-y0)/FT,w2=Math.toRadians(y1-YF)/T;
    om="($Time > <"+t0+" s> && $Time < <"+(t0+FT)+" s>) ? <"+w1+" radian/s> : (($Time >= <"+(t0+FT)+" s> && $Time < <"+(t0+FT+T)+" s>) ? <"+w2+" radian/s> : <0 radian/s>)";}
   List<String> rateM=new ArrayList<>();Object rate=null;
   Object spec=rm.getRotationSpecification();   // star.motion.RotationRate
   for(Method mt:spec.getClass().getMethods()){if(mt.getParameterCount()==0&&mt.getName().startsWith("get")&&!mt.getReturnType().isPrimitive()){
     try{Object o=mt.invoke(spec);if(o!=null&&hasMethod(o,"setDefinition")){rateM.add(mt.getName());if(rate==null)rate=o;}}catch(Throwable e){}}}
   log.put("ramp_rate_methods",rateM);
   if(rate==null)throw new IllegalStateException("brak predkosci obrotu w RotatingMotion: "+rateM);
   // 2026-11-22: wprost getRotationRate() - wybor przez refleksje bral czasem getRotationRateInput (nieuzywany obiekt) -> obrot = 0 (O00 v3)
   ScalarPhysicalQuantity rq=((RotationRate)spec).getRotationRate();rq.setDefinition(om);
   if(!om.equals(rq.getDefinition()))throw new IllegalStateException("predkosc obrotu nie ustawiona: "+rq.getDefinition());
   log.put("ramp_rate_check",rq.getDefinition());
   reg.getValues().get(MotionSpecification.class).setMotion(rm);
   log.put("ramp_motion","RotatingMotion os z przez (x="+XS+",0,0), omega="+om);
  }
  log.put("ramp","skret "+y0+" -> "+y1+" deg, t "+t0+" .. "+(t0+T)+" s, potem staly");
  Map<String,String> done=new LinkedHashMap<>();
  for(Boundary bb:reg.getBoundaryManager().getBoundaries()){
   String n=bb.getPresentationName();if(!n.equals("Block.side")&&!n.equals("Block.side2"))continue;
   AreaAverageReport ay=s.getReportManager().createReport(AreaAverageReport.class);ay.setFieldFunction(ff("Position").getComponentFunction(1));ay.getParts().setObjects(bb);
   double yy=ay.getReportMonitorValue();ay.setPresentationName("TF7_pom_bok_y_"+n);
   if(yy<0){bb.setBoundaryType(InletBoundary.class);
    bb.getConditions().get(InletVelocityOption.class).setSelected(InletVelocityOption.Type.COMPONENTS);
    vf(bb.getValues().get(VelocityProfile.class),wf(fw,"Velocity of "));frac(bb.getValues().get(VolumeFractionProfile.class),wf(fw,"Volume Fraction of Heavy Fluid of "));
    bb.getConditions().get(KwTurbSpecOption.class).setSelected(KwTurbSpecOption.Type.INTENSITY_VISCOSITY_RATIO);
    scalar(bb.getValues().get(TurbulenceIntensityProfile.class),.01);scalar(bb.getValues().get(TurbulentViscosityRatioProfile.class),10);
    soft("forcing side",()->{bb.getConditions().get(VofWaveForcingBoundaryOption.class).setSelected(VofWaveForcingBoundaryOption.Type.YES);scalar(bb.getValues().get(VofWaveForcingLength.class),0.6);});
    bb.setPresentationName("Block.bok -y (wlot przy skrecie)");done.put(n,"y="+yy+" -> velocity inlet");}
   else{bb.setBoundaryType(PressureBoundary.class);
    sf(bb.getValues().get(StaticPressureProfile.class),wf(fw,"Hydrostatic Pressure of Heavy Fluid of "));frac(bb.getValues().get(VolumeFractionProfile.class),wf(fw,"Volume Fraction of Heavy Fluid of "));
    soft("backflow",()->{bb.getConditions().get(BackflowSpecification.class).getBackflowVelocityDirectionOption().setSelected(BackflowVelocityDirectionOption.Type.EXTRAPOLATED);
     bb.getConditions().get(BackflowSpecification.class).getReversedFlowPressureOption().setSelected(ReversedFlowPressureOption.Type.ENVIRONMENTAL);});
    soft("damping side",()->{bb.getConditions().get(VofWaveDampingBoundaryOption.class).setSelected(VofWaveDampingBoundaryOption.Type.YES);scalar(bb.getValues().get(VofWaveDampingLength.class),0.8);});
    bb.setPresentationName("Block.bok +y (wylot przy skrecie)");done.put(n,"y="+yy+" -> pressure outlet");}
  }
  log.put("ramp_boundaries",done);
  ExpressionReport ang=s.getReportManager().createReport(ExpressionReport.class);ang.setPresentationName("TF7_skret_deg");
  if(FT>0)ang.setDefinition(y0+"+"+(YF-y0)+"*min(max(($Time-<"+t0+" s>)/<"+FT+" s>,0),1)+"+(y1-YF)+"*min(max(($Time-<"+(t0+FT)+" s>)/<"+T+" s>,0),1)");
  else ang.setDefinition(y0+"+"+(y1-y0)+"*min(max(($Time-<"+t0+" s>)/<"+T+" s>,0),1)");addMon(ang);plot("TF7 9 kat skretu [deg]",List.of("TF7_skret_deg"));
  write("SETUP.json",log);
 }
 // ---------- fizyka: VOF woda/powietrze + SST k-omega IDDES ----------
 VofWave flat;
 void vof(Region reg){
  PhysicsContinuum pc=s.getContinuumManager().createContinuum(PhysicsContinuum.class);pc.setPresentationName("TF7 VOF woda/powietrze, SST k-omega IDDES");
  pc.enable(ThreeDimensionalModel.class);pc.enable(ImplicitUnsteadyModel.class);pc.enable(EulerianMultiPhaseModel.class);
  EulerianMultiPhaseModel mp=pc.getModelManager().getModel(EulerianMultiPhaseModel.class);
  EulerianPhase water=mp.createPhase();water.setPresentationName("water");water.enable(SinglePhaseLiquidModel.class);water.enable(ConstantDensityModel.class);
  Material wm=water.getModelManager().getModel(SinglePhaseLiquidModel.class).getMaterial();
  wm.getMaterialProperties().getMaterialProperty(ConstantDensityProperty.class).setConstant(RHO);
  EulerianPhase air=mp.createPhase();air.setPresentationName("air");air.enable(SinglePhaseGasModel.class);air.enable(ConstantDensityModel.class);
  Material amat=air.getModelManager().getModel(SinglePhaseGasModel.class).getMaterial();
  amat.getMaterialProperties().getMaterialProperty(ConstantDensityProperty.class).setConstant(1.2);
  pc.enable(SegregatedVofModel.class);pc.enable(GravityModel.class);pc.enable(TurbulentModel.class);pc.enable(SegregatedVolumeFluxBasedFlowModel.class);
  String turb=cfg.getProperty("turbulence","iddes");
  if(turb.equals("iddes")){
   pc.enable(DesTurbulenceModel.class);pc.enable(SstKwTurbDesModel.class);pc.enable(KwAllYplusWallTreatment.class);
   SstKwTurbDesModel des=pc.getModelManager().getModel(SstKwTurbDesModel.class);
   List<String> opts=new ArrayList<>();
   try{for(Object o:DesFormulationOption.Type.class.getEnumConstants())opts.add(o.toString());}catch(Throwable e){}
   log.put("des_options",opts);
   des.getDesFormulationOption().setSelected(DesFormulationOption.Type.IDDES);
   log.put("des_formulation",des.getDesFormulationOption().getSelectedElement().toString());
  }else{
   pc.enable(RansTurbulenceModel.class);pc.enable(KOmegaTurbulence.class);pc.enable(SstKwTurbModel.class);pc.enable(KwAllYplusWallTreatment.class);
  }
  log.put("turbulence",turb);
  // Cell Quality Remediation: Y04 (2026-11-19) rozjechal sie od 1 zdegenerowanej komorki (skosnosc 158 deg) przy szczycie strutu w powietrzu
  if(!"false".equals(cfg.getProperty("cell_remediation"))){String cqr="brak";
   try{pc.enable(star.metrics.CellQualityRemediationModel.class);cqr="ON";}catch(Throwable e){cqr="blad: "+e;}
   log.put("cell_quality_remediation",cqr);if(cqr.equals("brak"))warn.add("CellQualityRemediationModel nie znaleziony");}
  wm.getMaterialProperties().getMaterialProperty(DynamicViscosityProperty.class).setConstant(MU);
  amat.getMaterialProperties().getMaterialProperty(DynamicViscosityProperty.class).setConstant(1.8e-5);
  reg.setPhysicsContinuum(pc);
  pc.getReferenceValues().get(Gravity.class).setComponents(0,0,-9.81);
  pc.getReferenceValues().get(ReferencePressure.class).setValue(101325);
  pc.getReferenceValues().get(ReferenceDensity.class).setValue(1.2);
  pc.getReferenceValues().get(ReferenceAltitude.class).setComponents(0,0,ZS);
  pc.getModelManager().getModel(SegregatedVolumeFluxBasedFlowModel.class).getUpwindOption().setSelected(FlowUpwindOption.Type.SECOND_ORDER);
  pc.getModelManager().getModel(SegregatedVofModel.class).setAngleFactor(.15);
  pc.enable(VofWaveModel.class);VofWaveModel vm=pc.getModelManager().getModel(VofWaveModel.class);
  FlatVofWave fw=vm.getVofWaveManager().createVofWave(FlatVofWave.class,"TF7 plaska woda");flat=fw;
  fw.setHeavyFluidDensity(RHO);fw.setLightFluidDensity(1.2);
  fw.getPointOnLevel().setComponents(0,0,ZS);fw.getVerticalDirection().setComponents(0,0,1);
  fw.getCurrent().setComponents(-U,0,0);fw.getWind().setComponents(-U,0,0);
  InitialConditionManager ic=pc.getInitialConditions();
  ic.get(KwTurbSpecOption.class).setSelected(KwTurbSpecOption.Type.INTENSITY_VISCOSITY_RATIO);
  scalar(ic.get(TurbulenceIntensityProfile.class),.01);scalar(ic.get(TurbulentViscosityRatioProfile.class),10);
  vf(ic.get(VelocityProfile.class),wf(fw,"Velocity of "));
  sf(ic.get(InitialPressureProfile.class),wf(fw,"Hydrostatic Pressure of Heavy Fluid of "));
  frac(ic.get(VolumeFractionProfile.class),wf(fw,"Volume Fraction of Heavy Fluid of "));
  reg.getConditions().get(VofWaveZoneOption.class).setSelected(VofWaveZoneOptionEnum.FORCING_DAMPING);
  reg.getConditions().get(ForcingLengthSpecificationOption.class).setSelected(LengthSpecificationOption.Type.BOUNDARY);
  reg.getConditions().get(DampingLengthSpecificationOption.class).setSelected(LengthSpecificationOption.Type.BOUNDARY);
  reg.getValues().get(ForcingVofWaveSpecification.class).setForcingVofWave(fw);
  Map<String,String> done=new LinkedHashMap<>();
  for(Boundary bb:reg.getBoundaryManager().getBoundaries()){
   String n=bb.getPresentationName();
   if(n.equals("Block.inlet")||n.equals("Block.bottom")){
    bb.setBoundaryType(InletBoundary.class);
    bb.getConditions().get(InletVelocityOption.class).setSelected(InletVelocityOption.Type.COMPONENTS);
    vf(bb.getValues().get(VelocityProfile.class),wf(fw,"Velocity of "));
    frac(bb.getValues().get(VolumeFractionProfile.class),wf(fw,"Volume Fraction of Heavy Fluid of "));
    bb.getConditions().get(KwTurbSpecOption.class).setSelected(KwTurbSpecOption.Type.INTENSITY_VISCOSITY_RATIO);
    scalar(bb.getValues().get(TurbulenceIntensityProfile.class),.01);scalar(bb.getValues().get(TurbulentViscosityRatioProfile.class),10);
    if(n.equals("Block.inlet")){bb.getConditions().get(VofWaveForcingBoundaryOption.class).setSelected(VofWaveForcingBoundaryOption.Type.YES);scalar(bb.getValues().get(VofWaveForcingLength.class),d("forcing_m",1.0));}
    done.put(n,"velocity inlet");
   }else if(n.equals("Block.outlet")||n.equals("Block.Block Surface")){
    bb.setBoundaryType(PressureBoundary.class);
    sf(bb.getValues().get(StaticPressureProfile.class),wf(fw,"Hydrostatic Pressure of Heavy Fluid of "));
    frac(bb.getValues().get(VolumeFractionProfile.class),wf(fw,"Volume Fraction of Heavy Fluid of "));
    bb.getConditions().get(BackflowSpecification.class).getBackflowVelocityDirectionOption().setSelected(BackflowVelocityDirectionOption.Type.EXTRAPOLATED);
    bb.getConditions().get(BackflowSpecification.class).getReversedFlowPressureOption().setSelected(ReversedFlowPressureOption.Type.ENVIRONMENTAL);
    if(n.equals("Block.outlet")){bb.getConditions().get(VofWaveDampingBoundaryOption.class).setSelected(VofWaveDampingBoundaryOption.Type.YES);scalar(bb.getValues().get(VofWaveDampingLength.class),d("damping_m",1.5));}
    done.put(n,"pressure outlet");
   }else if(n.startsWith("Block.")){bb.setBoundaryType(SymmetryBoundary.class);done.put(n,"symmetry (bok domeny, 2 m od plata)");}
   else done.put(n,"wall");
  }
  log.put("boundaries",done);
  ImplicitUnsteadySolver un=s.getSolverManager().getSolver(ImplicitUnsteadySolver.class);
  un.getTimeStep().setValue(d("dt_s",4e-4));
  soft("2nd order time",()->un.getTimeDiscretizationOption().setSelected(TimeDiscretizationOption.Type.SECOND_ORDER));
  for(SolverStoppingCriterion c:s.getSolverStoppingCriterionManager().getObjects()){
   if(c instanceof InnerIterationStoppingCriterion){((InnerIterationStoppingCriterion)c).setMaximumNumberInnerIterations(i("inner",5));c.setIsUsed(true);}
   else if(c instanceof PhysicalTimeStoppingCriterion){((PhysicalTimeStoppingCriterion)c).getMaximumTime().setValue(d("t_end_s",2.0));c.setIsUsed(true);}
   else if(c instanceof StepStoppingCriterion)c.setIsUsed(false);
  }
  write("SETUP.json",log);
 }

 // ---------- raporty TF7_* (pelny model: sily bez mnoznika) ----------
 List<Monitor> mons=new ArrayList<>(); Map<String,Monitor> mon=new LinkedHashMap<>();
 Report addMon(Report r){ReportMonitor rm=r.createMonitor();rm.setPresentationName(r.getPresentationName()+" Monitor");
  try{rm.getStarUpdate().getUpdateModeOption().setSelected(StarUpdateModeOption.Type.TIMESTEP);}catch(Throwable e){}
  mons.add(rm);mon.put(r.getPresentationName(),rm);return r;}
 ForceReport force(String name,Collection<? extends NamedObject> b,double[] dir,String opt){
  ForceReport r=s.getReportManager().createReport(ForceReport.class);r.setPresentationName(name);r.getParts().setObjects(b);
  r.getDirection().setComponents(dir[0],dir[1],dir[2]);
  try{r.getForceOption().setSelected(ForceReportForceOption.Type.valueOf(opt));}catch(Throwable e){warn.add("forceopt "+name);}
  addMon(r);return r;}
 UserFieldFunction uff(String name,String def){UserFieldFunction f=s.getFieldFunctionManager().createFieldFunction();f.setPresentationName(name);f.setFunctionName(name);
  f.getTypeOption().setSelected(FieldFunctionTypeOption.Type.SCALAR);f.setDefinition(def);return f;}
 void expr(String name,String def){ExpressionReport e=s.getReportManager().createReport(ExpressionReport.class);e.setPresentationName(name);e.setDefinition(def);addMon(e);}
 SurfaceIntegralReport integ(String name,FieldFunction f,Collection<? extends NamedObject> p){SurfaceIntegralReport r=s.getReportManager().createReport(SurfaceIntegralReport.class);r.setPresentationName(name);r.setFieldFunction(f);r.getParts().setObjects(p);addMon(r);return r;}
 MaxReport maxr(String name,FieldFunction f,Collection<? extends NamedObject> p){MaxReport r=s.getReportManager().createReport(MaxReport.class);r.setPresentationName(name);r.setFieldFunction(f);r.getParts().setObjects(p);addMon(r);return r;}
 MinReport minr(String name,FieldFunction f,Collection<? extends NamedObject> p){MinReport r=s.getReportManager().createReport(MinReport.class);r.setPresentationName(name);r.setFieldFunction(f);r.getParts().setObjects(p);addMon(r);return r;}
 FieldFunction cpF;
 void reports(Region region,Boundary foil,Boundary strut){
  List<Boundary> F=List.of(foil),S=List.of(strut),FS=List.of(foil,strut);List<Region> R=List.of(region);
  double q=0.5*RHO*U*U, qS=q*S_FULL;
  String phyd="(101325 + (($${Position}[2] < "+ZS+") ? "+RHO+" : 1.2)*9.81*("+ZS+" - $${Position}[2]))";
  cpF=uff("TF7_Cp","($AbsolutePressure - "+phyd+")/"+q);
  UserFieldFunction pz=uff("TF7_pdyn_nz","($AbsolutePressure - "+phyd+") * $${Area}[2] / mag($${Area})");
  integ("TF7_Lp_foil_dyn",pz,F);
  force("TF7_Ls_foil",F,new double[]{0,0,1},"SHEAR");
  expr("TF7_L_kg","(abs(${TF7_Lp_foil_dynReport}) + ${TF7_Ls_foilReport})/9.81");
  expr("TF7_CL","(abs(${TF7_Lp_foil_dynReport}) + ${TF7_Ls_foilReport})/"+qS);
  force("TF7_D_foil",F,new double[]{-1,0,0},"PRESSURE_AND_SHEAR");
  force("TF7_D_strut",S,new double[]{-1,0,0},"PRESSURE_AND_SHEAR");
  force("TF7_Dp_strut",S,new double[]{-1,0,0},"PRESSURE");
  expr("TF7_D_total","${TF7_D_foilReport}+${TF7_D_strutReport}");
  force("TF7_Fy_strut",S,new double[]{0,1,0},"PRESSURE_AND_SHEAR");
  force("TF7_Fy_foil",F,new double[]{0,1,0},"PRESSURE_AND_SHEAR");
  expr("TF7_Fy_total","${TF7_Fy_strutReport}+${TF7_Fy_foilReport}");
  MomentReport mz=s.getReportManager().createReport(MomentReport.class);mz.setPresentationName("TF7_Mz_os_skretu");mz.getParts().setObjects(FS);
  mz.getOrigin().setComponents(XS,0,0);mz.getDirection().setComponents(0,0,1);addMon(mz);
  MomentReport mx=s.getReportManager().createReport(MomentReport.class);mx.setPresentationName("TF7_Mx_przechyl");mx.getParts().setObjects(FS);
  mx.getOrigin().setComponents(XS,0,ZS);mx.getDirection().setComponents(1,0,0);addMon(mx);
  // wentylacja: powierzchnia sciany pokryta powietrzem ponizej lustra wody spokojnej (5 mm zapasu na falowanie lustra)
  UserFieldFunction ab=uff("TF7_powietrze_pod_lustrem","($${Position}[2] < "+(ZS-0.005)+") ? $VolumeFractionair : 0");
  UserFieldFunction sub=uff("TF7_ponizej_lustra","($${Position}[2] < "+(ZS-0.005)+") ? 1 : 0");
  integ("TF7_went_strut_m2",ab,S);integ("TF7_went_foil_m2",ab,F);integ("TF7_strut_pod_lustrem_m2",sub,S);
  expr("TF7_went_strut_pct","100*${TF7_went_strut_m2Report}/max(${TF7_strut_pod_lustrem_m2Report},1e-9)");
  minr("TF7_Cp_min_strut",cpF,S);minr("TF7_Cp_min_foil",cpF,F);
  UserFieldFunction top=uff("TF7_wysokosc_wody","($VolumeFractionwater > 0.5) ? $${Position}[2] - "+ZS+" : -9");
  maxr("TF7_max_woda_nad_lustrem_m",top,R);
  UserFieldFunction icf=uff("TF7_interface_Courant","($VolumeFractionwater>0.01 && $VolumeFractionwater<0.99) ? $CourantNumber : 0");
  maxr("TF7_interfaceCFL",icf,R);
  AreaAverageReport yp=s.getReportManager().createReport(AreaAverageReport.class);yp.setPresentationName("TF7_yplus_mean");yp.setFieldFunction(ff("WallYplus"));yp.getParts().setObjects(FS);addMon(yp);
  plot("TF7 1 sila nosna [kg]",List.of("TF7_L_kg"));
  plot("TF7 2 opor [N]: plat, strut, razem",List.of("TF7_D_foil","TF7_D_strut","TF7_Dp_strut","TF7_D_total"));
  plot("TF7 3 sila boczna [N]",List.of("TF7_Fy_strut","TF7_Fy_foil","TF7_Fy_total"));
  plot("TF7 4 momenty [Nm]: skret i przechyl",List.of("TF7_Mz_os_skretu","TF7_Mx_przechyl"));
  plot("TF7 5 wentylacja: % strutu pod lustrem w powietrzu",List.of("TF7_went_strut_pct"));
  plot("TF7 6 wentylacja: powierzchnia w powietrzu [m2]",List.of("TF7_went_strut_m2","TF7_went_foil_m2"));
  plot("TF7 7 Cp min (ssanie)",List.of("TF7_Cp_min_strut","TF7_Cp_min_foil"));
  plot("TF7 8 fala przy strucie [m] i CFL",List.of("TF7_max_woda_nad_lustrem_m","TF7_interfaceCFL"));
  List<String> rn=new ArrayList<>(mon.keySet());log.put("reports",rn);write("SETUP.json",log);
 }
 void plot(String name,List<String> keys){
  soft("plot "+name,()->{List<Monitor> l=new ArrayList<>();for(String k:keys)if(mon.containsKey(k))l.add(mon.get(k));
   MonitorPlot p=s.getPlotManager().createMonitorPlot(l,name);
   try{call(p,"setXAxisMonitor",s.getMonitorManager().getMonitor("Physical Time"));}catch(Throwable e){warn.add("x-axis time "+name);}
   try{p.getAxisManager().getAxis("Bottom Axis").getTitle().setText("czas [s]");}catch(Throwable e){}});
 }

 // ---------- sceny z zapisem animacji (natywne: Scene > Update > Save Animation) ----------
 Path anim;
 Object iso(String name,Region reg,FieldFunction f,double val){
  Object ip=null;
  for(Method mt:s.getPartManager().getClass().getMethods())if(mt.getName().equals("createIsoPart")&&mt.getParameterCount()==2){
   try{if(mt.getParameterTypes()[0]!=Vector.class)continue;ip=mt.invoke(s.getPartManager(),new Vector<Object>(List.of(reg)),f);break;}catch(Throwable e){warn.add("iso call: "+e.getCause());}}
  if(ip==null)throw new IllegalStateException("createIsoPart");
  call(ip,"setPresentationName",name);
  try{Class<?> ec=Class.forName("star.vis.IsoMode");for(Object c:ec.getEnumConstants())if(c.toString().toUpperCase().contains("SINGLE"))call(ip,"setMode",c);}catch(Throwable e){}
  ((star.vis.IsoPart)ip).getSingleIsoValue().setValue(val);
  return ip;
 }
 ScalarDisplayer sd(Scene sc,String name,Collection<?> parts,FieldFunction f,double lo,double hi,FvRepresentation fv){
  ScalarDisplayer d=sc.getDisplayerManager().createScalarDisplayer(name);d.getInputParts().setObjects((Collection)parts);d.setRepresentation(fv);
  d.getScalarDisplayQuantity().setFieldFunction(f);
  d.getScalarDisplayQuantity().setAutoRange(AutoRangeMode.NONE);d.getScalarDisplayQuantity().setRange(new DoubleVector(new double[]{lo,hi}));
  return d;
 }
 void view(Scene sc,double[] focal,double[] pos,double scale){view(sc,focal,pos,scale,new double[]{0,0,1});}
 void view(Scene sc,double[] focal,double[] pos,double scale,double[] up){
  sc.getCurrentView().setInput(new DoubleVector(focal),new DoubleVector(pos),new DoubleVector(up),scale,0,s.getCoordinateSystemManager().getLabCoordinateSystem(),false);
 }
 void animate(Scene sc,String base,int every){
  soft("anim "+base,()->{Object su=call(sc,"getSceneUpdate");
   Object um=call(su,"getUpdateModeOption");call(um,"setSelected",StarUpdateModeOption.Type.TIMESTEP);
   Object fq=call(su,"getTimeStepUpdateFrequency");call(fq,"setTimeSteps",every);
   Path d=anim.resolve(base);try{Files.createDirectories(d);}catch(Exception e){}
   call(su,"setAnimationFilePath",d.toString());call(su,"setAnimationFilenameBase",base);
   try{Object hp=call(su,"getHardcopyProperties");call(hp,"setCurrentResolutionWidth",1600);call(hp,"setCurrentResolutionHeight",900);}catch(Throwable e){warn.add("hardcopy size "+base);}
   call(su,"setSaveAnimation",true);});
 }
 void scenes(Region reg,Boundary foil,Boundary strut,FvRepresentation fv){
  anim=out.resolve("ANIMACJE");try{Files.createDirectories(anim);}catch(Exception e){}
  int ev=i("anim_every",25);
  List<Boundary> W=List.of(foil,strut);
  FieldFunction alphaW=ff("VolumeFractionwater"),alphaA=ff("VolumeFractionair");
  FieldFunction elev=ffPres("TF7_wzniesienie_lustra");if(elev==null)elev=uff("TF7_wzniesienie_lustra","$${Position}[2] - "+ZS);
  Object fs=null;try{fs=iso("TF7 powierzchnia swobodna (alfa wody = 0,5)",reg,alphaW,0.5);}catch(Throwable e){warn.add("iso FS: "+e);}
  Object qiso=null;
  FieldFunction qf=ffPres("Q-Criterion");if(qf==null)qf=ffPres("Q Criterion");
  if(qf!=null)try{qiso=iso("TF7 wiry (Q = "+d("q_iso",2e4)+" 1/s2)",reg,qf,d("q_iso",2e4));}catch(Throwable e){warn.add("iso Q: "+e);}else warn.add("brak pola Q-Criterion");
  double cx=0.13,L=0.9;
  // 01: powierzchnia swobodna z gory-z tylu, kolor = wzniesienie lustra
  { Scene sc=s.getSceneManager().createScene("TF7 01 powierzchnia swobodna - wzniesienie lustra");sc.initializeAndWait();
    PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("plat i strut");g.getInputParts().setObjects(W);g.setRepresentation(fv);
    if(fs!=null)sd(sc,"wzniesienie [m]",List.of(fs),elev,-0.05,0.05,fv);
    view(sc,new double[]{cx-0.15,0,ZS},new double[]{cx-0.75,-0.55,ZS+0.45},L);animate(sc,"01_powierzchnia",ev); }
  // 02/03: sciany w kolorze udzialu powietrza - wentylacja; widok z obu stron (strona ssaca zalezy od kierunku skretu)
  for(int sgn:new int[]{1,-1}){ Scene sc=s.getSceneManager().createScene("TF7 0"+(sgn>0?2:3)+" wentylacja - powietrze na scianach, widok od "+(sgn>0?"+y":"-y"));sc.initializeAndWait();
    sd(sc,"udzial powietrza [-]",W,alphaA,0,1,fv);
    if(fs!=null){ScalarDisplayer w=sd(sc,"lustro",List.of(fs),elev,-0.05,0.05,fv);try{w.setOpacity(0.35);}catch(Throwable e){}}
    view(sc,new double[]{0.12,0,ZS-0.04},new double[]{0.0,sgn*0.9,ZS+0.05},0.35);animate(sc,"0"+(sgn>0?2:3)+"_wentylacja_"+(sgn>0?"plus_y":"minus_y"),ev); }
  // 04: wiry Q kolor predkosc + lustro polprzezroczyste
  if(qiso!=null){ Scene sc=s.getSceneManager().createScene("TF7 04 wiry (Q) i lustro");sc.initializeAndWait();
    PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("plat i strut");g.getInputParts().setObjects(W);g.setRepresentation(fv);
    sd(sc,"predkosc [m/s]",List.of(qiso),ff("Velocity").getMagnitudeFunction(),0,1.3*U,fv);
    if(fs!=null){PartDisplayer w=sc.getDisplayerManager().createPartDisplayer("lustro");w.getInputParts().setObjects((Collection)List.of(fs));w.setRepresentation(fv);try{w.setOpacity(0.25);}catch(Throwable e){}}
    view(sc,new double[]{0.0,0,ZS-0.03},new double[]{-1.0,-0.7,ZS+0.5},0.6);animate(sc,"04_wiry",2*ev); }
  // 05: Cp na scianach
  { Scene sc=s.getSceneManager().createScene("TF7 05 Cp na placie i strucie");sc.initializeAndWait();
    sd(sc,"Cp [-]",W,cpF,-1.2,0.6,fv);
    view(sc,new double[]{0.12,0,ZS-0.04},new double[]{0.4,-0.7,ZS+0.4},0.5);animate(sc,"05_Cp",2*ev); }
  // 06: przekroj poziomy 2 cm pod lustrem: udzial powietrza (czy kieszen powietrza schodzi wzdluz strutu)
  soft("plane",()->{ PlaneSection pl=s.getPartManager().createPlaneSection();pl.setPresentationName("TF7 przekroj z = lustro - 20 mm");pl.getInputParts().setObjects(reg);
    pl.setOrigin(new DoubleVector(new double[]{0,0,ZS-0.02}));pl.setOrientation(new DoubleVector(new double[]{0,0,1}));
    Scene sc=s.getSceneManager().createScene("TF7 06 przekroj 20 mm pod lustrem - powietrze i predkosc");sc.initializeAndWait();
    sd(sc,"udzial powietrza [-]",List.of(pl),alphaA,0,1,fv);
    PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("strut");g.getInputParts().setObjects(List.of(strut));g.setRepresentation(fv);
    view(sc,new double[]{0.05,0,ZS-0.02},new double[]{0.05,0,ZS+0.8},0.35,new double[]{-1,0,0});animate(sc,"06_przekroj_powietrze",ev); });
  // 07: ten sam przekroj, predkosc
  soft("plane2",()->{ PlaneSection pl=(PlaneSection)s.getPartManager().getObject("TF7 przekroj z = lustro - 20 mm");
    Scene sc=s.getSceneManager().createScene("TF7 07 przekroj 20 mm pod lustrem - predkosc");sc.initializeAndWait();
    sd(sc,"predkosc [m/s]",List.of(pl),ff("Velocity").getMagnitudeFunction(),0,1.3*U,fv);
    view(sc,new double[]{0.05,0,ZS-0.02},new double[]{0.05,0,ZS+0.8},0.35,new double[]{-1,0,0});animate(sc,"07_przekroj_predkosc",2*ev); });
  List<String> sn=new ArrayList<>();for(Scene sc:s.getSceneManager().getScenes())if(sc.getPresentationName().startsWith("TF7"))sn.add(sc.getPresentationName());log.put("scenes",sn);
  write("SETUP.json",log);
 }
 // ---------- historia rozwiazania (GUI: wczytaj historie i przewijaj sceny) ----------
 void history(Region reg){
  try{
   String path=out.resolve(cfg.getProperty("name","TF7")+"_historia.simh").toAbsolutePath().toString().replace('\\','/');
   SolutionHistory sh=s.get(SolutionHistoryManager.class).createForFile(path,false);
   sh.setPresentationName("TF7 historia (alfa, predkosc, cisnienie, Cp, Q)");
   try{call(sh.getInputs(),"setQuery",new Object[]{null});}catch(Throwable e){warn.add("history setQuery: "+e);}
   sh.getInputs().setObjects(reg);
   sh.getRegions().setQuery(null);sh.getRegions().setObjects(reg);
   if(sh.getRegions().getObjects().isEmpty())warn.add("history: puste regiony");
   log.put("history_regions",String.valueOf(sh.getRegions().getObjects()));
   List<Object> fl=new ArrayList<>(List.of(ff("VolumeFractionair"),ff("VolumeFractionwater"),ff("Velocity"),ff("Pressure"),cpF));
   FieldFunction qf=ffPres("Q-Criterion");if(qf!=null)fl.add(qf);
   sh.setFunctions(new NeoObjectVector(fl.toArray()));
   sh.getUpdate().getUpdateModeOption().setSelected(StarUpdateModeOption.Type.TIMESTEP);
   sh.getUpdate().getTimeStepUpdateFrequency().setTimeSteps(i("history_every",50));
   log.put("history_path",path);log.put("history_inputs",String.valueOf(sh.getInputs().getObjects()));
   try{log.put("history_functions",String.valueOf(call(sh,"getFunctions")));}catch(Throwable e){}
   log.put("history_every",i("history_every",50));
  }catch(Throwable e){warn.add("history: "+e);}
 }
 // ---------- dodatkowe sceny H (nagrywane na zywo): zblizenia, przekroje, linie pradu ----------
 Object streams;
 Object line(String nm,Region reg,double[] a,double[] b){
  for(Method mt:s.getPartManager().getClass().getMethods())if(mt.getName().equals("createLinePart")&&mt.getParameterCount()==4&&mt.getParameterTypes()[0]==Vector.class){
   try{Object lp=mt.invoke(s.getPartManager(),new NeoObjectVector(new Object[]{reg}),new DoubleVector(a),new DoubleVector(b),4);call(lp,"setPresentationName",nm);return lp;}catch(Throwable e){warn.add("line "+e.getCause());}}
  return null;
 }
 Object makeStreams(Region reg){
  List<Object> seeds=new ArrayList<>();
  for(double dz:new double[]{0.02,0.05}){
   seeds.add(line("TF7 posiew przed strutem "+Math.round(dz*1000)+" mm pod lustrem",reg,new double[]{0.30,-0.09,ZS-dz},new double[]{0.30,0.09,ZS-dz}));
   seeds.add(line("TF7 posiew z boku (-y) "+Math.round(dz*1000)+" mm pod lustrem",reg,new double[]{0.02,-0.12,ZS-dz},new double[]{0.32,-0.12,ZS-dz}));}
  seeds.removeIf(Objects::isNull);if(seeds.isEmpty())return null;
  for(Method mt:s.getPartManager().getClass().getMethods())if(mt.getName().equals("createStreamPart")&&mt.getParameterCount()==6&&mt.getParameterTypes()[1]==List.class&&mt.getParameterTypes()[3]==int.class){
   try{Object sp=mt.invoke(s.getPartManager(),new NeoObjectVector(new Object[]{reg}),new NeoObjectVector(seeds.toArray()),ff("Velocity"),1,20,0);call(sp,"setPresentationName","TF7 linie pradu");return sp;}catch(Throwable e){warn.add("stream "+e.getCause());}}
  return null;
 }
 void addStreams(Scene sc,FvRepresentation fv){
  if(streams==null)return;
  soft("stream displayer",()->{StreamDisplayer d=sc.getDisplayerManager().createStreamDisplayer("linie pradu");d.getInputParts().setObjects((Collection)List.of(streams));d.setRepresentation(fv);
   try{d.setWidth(0.0015);}catch(Throwable e){}
   try{Object q=call(d,"getScalarDisplayQuantity");call(q,"setFieldFunction",ff("Velocity").getMagnitudeFunction());call(q,"setAutoRange",AutoRangeMode.NONE);call(q,"setRange",new DoubleVector(new double[]{0,1.3*U}));}catch(Throwable e){}});
 }
 void labels(Scene sc){
  soft("label time",()->{Object am=s.getAnnotationManager();Object a=call(am,"getObject","Solution Time");Object pm=call(sc,"getAnnotationPropManager");call(pm,"createPropForAnnotation",a);});
  soft("label angle",()->{Report r=s.getReportManager().getReport("TF7_skret_deg");if(r==null)return;Object am=s.getAnnotationManager();Object a=null;
   try{a=call(am,"getObject","TF7_skret_deg");}catch(Throwable e){}
   if(a==null){a=call(am,"createReportAnnotation",r);call(a,"setPresentationName","TF7_skret_deg");}
   Object pm=call(sc,"getAnnotationPropManager");Object p=call(pm,"createPropForAnnotation",a);try{call(p,"setPosition",new DoubleVector(new double[]{0.02,0.92,0}));}catch(Throwable e){}});
 }
 void hScenes(Region reg,Boundary foil,Boundary strut,FvRepresentation fv){
  int ev=i("anim_every",25);List<Boundary> W=List.of(foil,strut);
  FieldFunction air=ff("VolumeFractionair"),water=ff("VolumeFractionwater"),vel=ff("Velocity").getMagnitudeFunction();
  FieldFunction elev=ffPres("TF7_wzniesienie_lustra");
  Object fs=null;for(Object o:s.getPartManager().getObjects())if(((NamedObject)o).getPresentationName().startsWith("TF7 powierzchnia swobodna"))fs=o;
  Object qi=null;for(Object o:s.getPartManager().getObjects())if(((NamedObject)o).getPresentationName().startsWith("TF7 wiry"))qi=o;
  streams=makeStreams(reg);double xs=0.15;
  for(int sg:new int[]{1,-1}){Scene sc=s.getSceneManager().createScene("TF7 H0"+(sg>0?1:2)+" strut z bliska, powietrze + linie pradu, od "+(sg>0?"+y":"-y"));sc.initializeAndWait();
   sd(sc,"powietrze",W,air,0,1,fv);if(fs!=null){ScalarDisplayer w=sd(sc,"lustro",List.of(fs),elev,-0.06,0.06,fv);try{w.setOpacity(0.3);}catch(Throwable e){}}
   addStreams(sc,fv);labels(sc);view(sc,new double[]{xs,0,ZS-0.03},new double[]{xs+0.05,sg*0.45,ZS-0.01},0.3);animate(sc,"H0"+(sg>0?1:2)+"_strut_"+(sg>0?"plus_y":"minus_y"),ev);}
  {Scene sc=s.getSceneManager().createScene("TF7 H03 widok z dolu, powietrze na scianach + linie pradu");sc.initializeAndWait();sd(sc,"powietrze",W,air,0,1,fv);
   if(fs!=null){ScalarDisplayer w=sd(sc,"lustro",List.of(fs),elev,-0.06,0.06,fv);try{w.setOpacity(0.25);}catch(Throwable e){}}
   addStreams(sc,fv);labels(sc);view(sc,new double[]{xs,0,ZS-0.05},new double[]{xs-0.25,-0.35,ZS-0.55},0.3);animate(sc,"H03_z_dolu",ev);}
  int k=4;for(double dz:new double[]{0.010,0.030,0.055}){final int kk=k;final double z=dz;
   soft("plane "+dz,()->{PlaneSection p=s.getPartManager().createPlaneSection();p.setPresentationName("TF7 przekroj "+Math.round(z*1000)+" mm pod lustrem");p.getInputParts().setObjects(reg);
    p.setOrigin(new DoubleVector(new double[]{0,0,ZS-z}));p.setOrientation(new DoubleVector(new double[]{0,0,1}));
    Scene sc=s.getSceneManager().createScene("TF7 H0"+kk+" przekroj "+Math.round(z*1000)+" mm pod lustrem - powietrze");sc.initializeAndWait();
    sd(sc,"powietrze",List.of(p),air,0,1,fv);PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("strut");g.getInputParts().setObjects(List.of(strut));g.setRepresentation(fv);
    labels(sc);view(sc,new double[]{0.05,0,ZS-z},new double[]{0.05,0,ZS+0.7},0.3,new double[]{-1,0,0});animate(sc,"H0"+kk+"_przekroj_"+Math.round(z*1000)+"mm",ev);});k++;}
  soft("plane x",()->{PlaneSection p=s.getPartManager().createPlaneSection();p.setPresentationName("TF7 przekroj pionowy x = 0,15");p.getInputParts().setObjects(reg);
   p.setOrigin(new DoubleVector(new double[]{xs,0,0}));p.setOrientation(new DoubleVector(new double[]{1,0,0}));
   Scene sc=s.getSceneManager().createScene("TF7 H07 przekroj pionowy w poprzek strutu - powietrze");sc.initializeAndWait();
   sd(sc,"powietrze",List.of(p),air,0,1,fv);PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("plat i strut");g.getInputParts().setObjects(W);g.setRepresentation(fv);
   labels(sc);view(sc,new double[]{xs,0,ZS-0.04},new double[]{xs+0.8,0,ZS-0.04},0.3);animate(sc,"H07_przekroj_pionowy",ev);});
  soft("plane y",()->{PlaneSection p=s.getPartManager().createPlaneSection();p.setPresentationName("TF7 przekroj pionowy y = 0");p.getInputParts().setObjects(reg);
   p.setOrigin(new DoubleVector(new double[]{0,0,0}));p.setOrientation(new DoubleVector(new double[]{0,1,0}));
   Scene sc=s.getSceneManager().createScene("TF7 H08 przekroj pionowy wzdluz (y=0) - powietrze");sc.initializeAndWait();
   sd(sc,"powietrze",List.of(p),air,0,1,fv);PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("plat i strut");g.getInputParts().setObjects(W);g.setRepresentation(fv);
   labels(sc);view(sc,new double[]{0.0,0,ZS-0.03},new double[]{0.0,-0.9,ZS-0.03},0.4);animate(sc,"H08_przekroj_wzdluz",2*ev);});
  if(qi!=null){final Object Q=qi,F=fs;soft("H09",()->{Scene sc=s.getSceneManager().createScene("TF7 H09 wiry (Q) z tylu-z dolu + linie pradu");sc.initializeAndWait();
   sd(sc,"Q",List.of(Q),vel,0,1.3*U,fv);PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("plat i strut");g.getInputParts().setObjects(W);g.setRepresentation(fv);
   addStreams(sc,fv);labels(sc);view(sc,new double[]{0.05,0,ZS-0.04},new double[]{-0.6,-0.5,ZS-0.25},0.5);animate(sc,"H09_wiry_z_dolu",2*ev);});}
  if(fs!=null){final Object F=fs;soft("H10",()->{Scene sc=s.getSceneManager().createScene("TF7 H10 lustro z gory - wzniesienie + linie pradu");sc.initializeAndWait();
   sd(sc,"lustro",List.of(F),elev,-0.06,0.06,fv);PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("plat i strut");g.getInputParts().setObjects(W);g.setRepresentation(fv);
   addStreams(sc,fv);labels(sc);view(sc,new double[]{-0.15,0,ZS},new double[]{-0.15,0,ZS+1.6},0.6,new double[]{-1,0,0});animate(sc,"H10_lustro_z_gory",ev);});}
  for(int sg:new int[]{1,-1}){final int g2=sg;soft("H11",()->{Scene sc=s.getSceneManager().createScene("TF7 H1"+(g2>0?1:2)+" Cp na scianach od "+(g2>0?"+y":"-y"));sc.initializeAndWait();
   sd(sc,"Cp",W,cpF,-1.5,0.8,fv);labels(sc);view(sc,new double[]{xs,0,ZS-0.04},new double[]{xs,g2*0.6,ZS+0.1},0.3);animate(sc,"H1"+(g2>0?1:2)+"_Cp_"+(g2>0?"plus_y":"minus_y"),2*ev);});}
  // etykiety czasu i kata tez na scenach 01-07
  for(Scene sc:s.getSceneManager().getScenes())if(sc.getPresentationName().matches("TF7 0\\d.*"))labels(sc);
  List<String> sn=new ArrayList<>();for(Scene sc:s.getSceneManager().getScenes())if(sc.getPresentationName().startsWith("TF7"))sn.add(sc.getPresentationName());log.put("scenes",sn);
  write("SETUP.json",log);
 }
}
