package macro;
import star.common.*;import star.base.neo.*;import star.meshing.*;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;

/** Sonda ustawien siatki (AutoMeshOperation: meshery, wartosci domyslne, kontrolki) + tabela scian strutu/plata (x,y,z,pole). */
public class TFoilMeshProbe extends StarMacro {
 List<String> L=new ArrayList<>();
 String val(Object o){
  StringBuilder b=new StringBuilder();
  for(String m:new String[]{"getValue","getRawValue","getRelativeSize","getAbsoluteSize","getNumLayers","getStretching","getRelativeOrAbsoluteOption","getSelectedElement","getEnabled"}){
   try{Method mm=o.getClass().getMethod(m);Object r=mm.invoke(o);
    if(r==null)continue;String rs=r.toString();
    try{Method p=r.getClass().getMethod("getPercentage");rs=p.invoke(r)+"%";}catch(Throwable e){}
    try{Method p=r.getClass().getMethod("getValue");if(!m.equals("getValue"))rs+=" ("+p.invoke(r)+")";}catch(Throwable e){}
    b.append(m.substring(3)).append("=").append(rs).append(" ");}catch(Throwable e){}
  }
  return b.toString();
 }
 void dump(String pre,Object mgr){
  try{Method g=mgr.getClass().getMethod("getObjects");for(Object o:(Collection<?>)g.invoke(mgr)){
   String n;try{n=((NamedObject)o).getPresentationName();}catch(Throwable e){n=o.getClass().getSimpleName();}
   L.add(pre+n+" ["+o.getClass().getSimpleName()+"] "+val(o));}}catch(Throwable e){L.add(pre+"? "+e);}
 }
 public void execute(){
  Simulation s=getActiveSimulation();Path out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));
  try{
   for(MeshOperation op:s.get(MeshOperationManager.class).getObjects()){
    L.add("== OP "+op.getPresentationName()+" ["+op.getClass().getSimpleName()+"]");
    if(op instanceof AutoMeshOperation){AutoMeshOperation am=(AutoMeshOperation)op;
     try{L.add(" parts: "+am.getInputGeometryObjects().getObjects());}catch(Throwable e){}
     dump(" mesher: ",am.getMeshers());
     dump(" default: ",am.getDefaultValues());
     for(CustomMeshControl c:am.getCustomMeshControls().getObjects()){
      L.add(" CTRL "+c.getPresentationName()+" ["+c.getClass().getSimpleName()+"]");
      try{L.add("   obj: "+((GeometryObjectGroup)c.getClass().getMethod("getGeometryObjects").invoke(c)).getObjects());}catch(Throwable e){}
      try{dump("   cond: ",c.getClass().getMethod("getCustomConditions").invoke(c));}catch(Throwable e){}
      try{dump("   val: ",c.getClass().getMethod("getCustomValues").invoke(c));}catch(Throwable e){}
     }
    }
   }
   for(GeometryPart gp:s.getGeometryPartManager().getParts()){
    L.add("== PART "+gp.getPresentationName()+" ["+gp.getClass().getSimpleName()+"]");
    try{for(PartSurface ps:gp.getPartSurfaces())L.add("  surf "+ps.getPresentationName());}catch(Throwable e){}
    try{for(PartCurve pc:gp.getPartCurves())L.add("  curve "+pc.getPresentationName());}catch(Throwable e){}
   }
   // tabela scian scian plata+strutu
   Region reg=s.getRegionManager().getRegions().iterator().next();List<Boundary> W=new ArrayList<>();
   for(Boundary b:reg.getBoundaryManager().getBoundaries())if(b.getBoundaryType() instanceof WallBoundary)W.add(b);
   L.add("walls: "+W);
   UserFieldFunction a=s.getFieldFunctionManager().createFieldFunction();a.setPresentationName("P_area");a.setFunctionName("P_area");a.setDefinition("mag($$Area)");
   XyzInternalTable t=s.getTableManager().createTable(XyzInternalTable.class);t.setFieldFunctions(new NeoObjectVector(new Object[]{a}));t.getParts().setObjects(W);t.extract();
   t.export(out.resolve("FACES.csv").toString(),",");
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(out.resolve("MESH_PROBE.txt"),L);}catch(Exception e){}
 }
}
