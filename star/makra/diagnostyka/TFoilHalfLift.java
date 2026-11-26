package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;
import java.nio.file.*;import java.util.*;

/** Sila nosna i powietrze osobno na dwoch polowach plata (podzial w ukladzie plata: y' = -(x-XS) sin b + y cos b). Bez liczenia. */
public class TFoilHalfLift extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();List<String> L=new ArrayList<>();
  try{
   double XS=0.16,q=0.5*997.0*8.333*8.333;
   double b=Math.toRadians(s.getReportManager().getReport("TF7_skret_deg").getReportMonitorValue());
   L.add(String.format("t=%.4f s, skret=%.2f deg",s.getSolution().getPhysicalTime(),Math.toDegrees(b)));
   MinReport cpr=(MinReport)s.getReportManager().getReport("TF7_Cp_min_foil");
   Collection<? extends NamedObject> F=cpr.getParts().getObjects();
   String yp="(-($$Position[0]-"+XS+")*"+Math.sin(b)+"+$$Position[1]*"+Math.cos(b)+")";
   String aw=null;for(FieldFunction f:s.getFieldFunctionManager().getObjects())if(f.getPresentationName().equals("Volume Fraction of air"))aw=f.getFunctionName();
   String[][] halves={{"y'>0 (strona +y plata)",yp+" > 0"},{"y'<0 (strona -y plata)",yp+" < 0"}};
   double tot=0;
   for(String[] hh:halves){
    UserFieldFunction lz=s.getFieldFunctionManager().createFieldFunction();String n="HL_"+L.size();lz.setFunctionName(n);lz.setPresentationName(n);
    lz.setDefinition("("+hh[1]+") ? (-$TF7_Cp*"+q+"*$$Area[2]) : 0");
    SumReport r=s.getReportManager().createReport(SumReport.class);r.setFieldFunction(lz);r.getParts().setObjects(F);double v=r.getReportMonitorValue();
    UserFieldFunction ar=s.getFieldFunctionManager().createFieldFunction();ar.setFunctionName(n+"a");ar.setPresentationName(n+"a");ar.setDefinition("("+hh[1]+") ? mag($$Area) : 0");
    SumReport ra=s.getReportManager().createReport(SumReport.class);ra.setFieldFunction(ar);ra.getParts().setObjects(F);double A=ra.getReportMonitorValue();
    double air=Double.NaN;
    if(aw!=null){UserFieldFunction aa=s.getFieldFunctionManager().createFieldFunction();aa.setFunctionName(n+"w");aa.setPresentationName(n+"w");aa.setDefinition("("+hh[1]+") ? mag($$Area)*$"+aw+" : 0");
     SumReport rw=s.getReportManager().createReport(SumReport.class);rw.setFieldFunction(aa);rw.getParts().setObjects(F);air=rw.getReportMonitorValue();}
    tot+=v;
    L.add(String.format("%s: Lz_cisn=%.1f N (%.2f kg), pole scian=%.0f cm2, pole*alfa_powietrza=%.1f cm2",hh[0],v,v/9.81,A*1e4,air*1e4));
   }
   L.add(String.format("suma polowek=%.1f N (%.2f kg); raport TF7_L_kg=%.2f",tot,tot/9.81,s.getReportManager().getReport("TF7_L_kg").getReportMonitorValue()));
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"HALF_LIFT.txt"),L);}catch(Exception e){}
 }
}
