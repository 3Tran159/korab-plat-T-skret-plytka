package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;import star.motion.*;
import java.nio.file.*;import java.util.*;
/** ProbeRot: czy siatka sie obraca? czas, kat (raport), y min/max scian strutu, ocena wyrazenia predkosci obrotu. -> ROT_PROBE.txt */
public class ProbeRot extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();List<String> L=new ArrayList<>();
  try{
   L.add("time="+s.getSolution().getPhysicalTime());
   Report a=s.getReportManager().getReport("TF7_skret_deg");if(a!=null)L.add("TF7_skret_deg="+a.getReportMonitorValue());
   Region reg=s.getRegionManager().getRegion("Subtract");
   Boundary st=reg.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace2");
   FieldFunction py=s.getFieldFunctionManager().getFunction("Position").getComponentFunction(1);
   FieldFunction px=s.getFieldFunctionManager().getFunction("Position").getComponentFunction(0);
   MaxReport mx=s.getReportManager().createReport(MaxReport.class);mx.setFieldFunction(py);mx.getParts().setObjects(st);L.add("strut y max="+mx.getReportMonitorValue());
   MinReport mn=s.getReportManager().createReport(MinReport.class);mn.setFieldFunction(py);mn.getParts().setObjects(st);L.add("strut y min="+mn.getReportMonitorValue());
   MaxReport xx=s.getReportManager().createReport(MaxReport.class);xx.setFieldFunction(px);xx.getParts().setObjects(st);L.add("strut x max="+xx.getReportMonitorValue());
   for(Object o:s.get(MotionManager.class).getObjects())if(o instanceof RotatingMotion){RotatingMotion rm=(RotatingMotion)o;
    String def=((RotationRate)rm.getRotationSpecification()).getRotationRate().getDefinition();L.add("rate def="+def);
    ExpressionReport e=s.getReportManager().createReport(ExpressionReport.class);e.setDefinition(def);L.add("rate value now="+e.getReportMonitorValue());}
   L.add("region motion="+reg.getValues().get(MotionSpecification.class).getMotion());
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"ROT_PROBE.txt"),L);}catch(Exception e){}
 }
}
