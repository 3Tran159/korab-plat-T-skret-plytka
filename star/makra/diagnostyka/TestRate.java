package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;import star.motion.*;
import java.nio.file.*;import java.util.*;
/** TestRate: proba definicji dwuetapowej predkosci obrotu (bez zagniezdzonego ?:), odczyt zwrotny. -> RATE_TEST.txt */
public class TestRate extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();List<String> L=new ArrayList<>();
  String[] defs={
   "(($Time > 0.15 && $Time < 0.4) ? 1.0471975511965976 : 0) + (($Time > 0.4 && $Time < 1.3333) ? 0.26180873811122485 : 0)",
   "($Time > <0.15 s> && $Time < <0.4 s>) ? <1.0471975511965976 radian/s> : (($Time >= <0.4 s> && $Time < <1.3333 s>) ? <0.26180873811122485 radian/s> : <0 radian/s>)",
   "(($Time > <0.15 s> && $Time < <0.4 s>) ? <1.0471975511965976 radian/s> : <0 radian/s>) + (($Time > <0.4 s> && $Time < <1.3333 s>) ? <0.26180873811122485 radian/s> : <0 radian/s>)"};
  try{
   for(Object o:s.get(MotionManager.class).getObjects())if(o instanceof RotatingMotion){RotatingMotion rm=(RotatingMotion)o;
    ScalarPhysicalQuantity q=((RotationRate)rm.getRotationSpecification()).getRotationRate();
    for(String d:defs){try{q.setDefinition(d);L.add("SET ok: "+d);}catch(Throwable e){L.add("SET blad: "+d+" -> "+e);}
     L.add("  odczyt: "+q.getDefinition());
     try{ExpressionReport e=s.getReportManager().createReport(ExpressionReport.class);e.setDefinition(q.getDefinition());L.add("  wartosc t=0,2: "+e.getReportMonitorValue());}catch(Throwable e){L.add("  ocena: "+e);}}}
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"RATE_TEST.txt"),L);}catch(Exception e){}
 }
}
