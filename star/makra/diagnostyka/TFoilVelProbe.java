package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;
import java.nio.file.*;import java.util.*;

/** Sonda: srednie skladowe predkosci na brzegach domeny i w punktach przed platem, w punkcie wznowienia. Bez liczenia. */
public class TFoilVelProbe extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();Path out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));
  List<String> L=new ArrayList<>();
  try{
   L.add("t="+s.getSolution().getPhysicalTime());
   Region reg=s.getRegionManager().getRegions().iterator().next();
   FieldFunction v=s.getFieldFunctionManager().getFunction("Velocity");
   for(Boundary b:reg.getBoundaryManager().getBoundaries()){
    StringBuilder sb=new StringBuilder(b.getPresentationName()+" ["+b.getBoundaryType().getPresentationName()+"] Uavg=");
    for(int i=0;i<3;i++){AreaAverageReport r=s.getReportManager().createReport(AreaAverageReport.class);r.setFieldFunction(v.getComponentFunction(i));r.getParts().setObjects(b);sb.append(String.format("%.3f ",r.getReportMonitorValue()));s.getReportManager().removeObjects(r);}
    L.add(sb.toString());
   }
   // objetosc: srednia predkosc w pudelkach (woda) przed platem i z boku
   double[][] boxes={{0.6,0.9,-0.1,0.1,-0.10,0.0},{-0.2,0.2,-0.6,-0.4,-0.10,0.0},{-0.2,0.2,0.4,0.6,-0.10,0.0},{-1.5,-1.0,-0.1,0.1,-0.10,0.0}};
   for(double[] bx:boxes){
    UserFieldFunction m=s.getFieldFunctionManager().createFieldFunction();String nm="P_box"+L.size();m.setFunctionName(nm);m.setPresentationName(nm);
    m.setDefinition("($$Position[0]>"+bx[0]+" && $$Position[0]<"+bx[1]+" && $$Position[1]>"+bx[2]+" && $$Position[1]<"+bx[3]+" && $$Position[2]>"+bx[4]+" && $$Position[2]<"+bx[5]+") ? 1 : 0");
    StringBuilder sb=new StringBuilder("box "+Arrays.toString(bx)+" U=");
    SumReport n=s.getReportManager().createReport(SumReport.class);n.setFieldFunction(m);n.getParts().setObjects(reg);double nn=n.getReportMonitorValue();
    for(int i=0;i<3;i++){UserFieldFunction u=s.getFieldFunctionManager().createFieldFunction();u.setFunctionName(nm+"u"+i);u.setPresentationName(nm+"u"+i);u.setDefinition("$"+nm+"*$$Velocity["+i+"]");
     SumReport r=s.getReportManager().createReport(SumReport.class);r.setFieldFunction(u);r.getParts().setObjects(reg);sb.append(String.format("%.3f ",r.getReportMonitorValue()/Math.max(nn,1)));}
    L.add(sb.append(" n=").append(nn).toString());
   }
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(out.resolve("VEL_PROBE.txt"),L);}catch(Exception e){}
 }
}
