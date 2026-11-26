package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;import star.vis.*;import star.post.*;
import java.nio.file.*;import java.io.*;import java.util.*;

/**
 * TFoilDiag - diagnostyka rozbieznosci Y04. Wczytuje *_SETUP.sim, liczy do t_diag (domyslnie 0,14 s)
 * i co 25 krokow zapisuje do DIAG.csv: Cp min na strucie i w objetosci, maks. predkosc,
 * oraz POLOZENIE (srednie x,y,z) komorek/scian z ekstremum. Na starcie: jakosc siatki.
 * Nie zapisuje historii ani animacji. Na koncu zapis Y04_DIAG.sim do obejrzenia w GUI.
 */
public class TFoilDiag extends StarMacro {
 Simulation s; Path out; StringBuilder txt=new StringBuilder();
 void p(String x){txt.append(x).append("\n");try{Files.writeString(out.resolve("DIAG.txt"),txt.toString());}catch(Exception e){}}
 FieldFunction ffp(String pres){for(FieldFunction f:s.getFieldFunctionManager().getObjects())if(f.getPresentationName().equals(pres))return f;return null;}
 UserFieldFunction uff(String n,String d){UserFieldFunction f=s.getFieldFunctionManager().createFieldFunction();f.setPresentationName(n);f.setFunctionName(n);f.setDefinition(d);return f;}
 <T extends Report> T rep(Class<T> c,String n,FieldFunction f,Collection<? extends NamedObject> parts){T r=s.getReportManager().createReport(c);r.setPresentationName(n);
  try{c.getMethod("setFieldFunction",FieldFunction.class).invoke(r,f);}catch(Exception e){throw new RuntimeException(e);}
  try{Object pp=c.getMethod("getParts").invoke(r);pp.getClass().getMethod("setObjects",Collection.class).invoke(pp,parts);}catch(Exception e){throw new RuntimeException(e);}
  return r;}
 double v(Report r){try{return r.getReportMonitorValue();}catch(Throwable e){return Double.NaN;}}

 public void execute(){
  s=getActiveSimulation();out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR",System.getProperty("user.dir")));
  try{run();}catch(Throwable e){StringWriter w=new StringWriter();e.printStackTrace(new PrintWriter(w));p("ERROR "+w);}
 }
 void run() throws Exception{
  double tEnd=Double.parseDouble(System.getenv().getOrDefault("TF_DIAG_T","0.14"));
  s.getSimulationIterator().getAutoSave().setEnabled(false);
  for(SolutionHistory sh:s.get(SolutionHistoryManager.class).getObjects()){try{sh.setAutoRecord(false);p("historia wylaczona: "+sh.getPresentationName());}catch(Throwable e){p("historia: "+e);}}
  for(Scene sc:s.getSceneManager().getScenes()){try{sc.getSceneUpdate().setSaveAnimation(false);}catch(Throwable e){}}
  Region reg=s.getRegionManager().getRegions().iterator().next();List<Region> R=List.of(reg);
  MinReport cps=(MinReport)s.getReportManager().getReport("TF7_Cp_min_strut");
  Collection<? extends NamedObject> S=cps.getParts().getObjects();
  p("strut: "+S);
  FieldFunction cp=s.getFieldFunctionManager().getFunction("TF7_Cp");
  // jakosc siatki
  for(String[] q:new String[][]{{"CellQuality","min"},{"VolumeChange","min"},{"SkewnessAngle","max"}}){
   try{FieldFunction f=s.getFieldFunctionManager().getFunction(q[0]);if(f==null){p(q[0]+": brak");continue;}
    Report r=q[1].equals("min")?rep(MinReport.class,"D_"+q[0],f,R):rep(MaxReport.class,"D_"+q[0],f,R);
    p("siatka "+q[0]+" "+q[1]+" = "+v(r));}catch(Throwable e){p(q[0]+": "+e);}
  }
  for(String[] q:new String[][]{{"CellQuality","<","1e-3"},{"VolumeChange","<","1e-3"},{"SkewnessAngle",">","85"}}){
   try{UserFieldFunction m=uff("D_cnt_"+q[0],"($"+q[0]+" "+q[1]+" "+q[2]+") ? 1 : 0");
    p("siatka liczba komorek "+q[0]+q[1]+q[2]+" = "+v(rep(SumReport.class,"D_sum_"+q[0],m,R)));}catch(Throwable e){p("cnt "+q[0]+": "+e);}
  }
  s.getSolution().initializeSolution();
  // maski i polozenia
  UserFieldFunction mS=uff("D_mS","0"),mV=uff("D_mV","0"),mU=uff("D_mU","0");
  UserFieldFunction speed=uff("D_speed","mag($$Velocity)");
  MinReport cpV=rep(MinReport.class,"D_Cp_min_vol",cp,R);MaxReport uMax=rep(MaxReport.class,"D_U_max",speed,R);
  FieldFunction aw=ffp("Volume Fraction of water");
  String[] ax={"x","y","z"};
  Map<String,Report> a=new LinkedHashMap<>();
  a.put("S_n",rep(SumReport.class,"D_S_n",mS,S));
  a.put("V_n",rep(SumReport.class,"D_V_n",mV,R));a.put("U_n",rep(SumReport.class,"D_U_n",mU,R));
  for(int i=0;i<3;i++){
   a.put("S_"+ax[i],rep(SumReport.class,"D_S_"+ax[i],uff("D_mSx"+i,"$D_mS*$$Position["+i+"]"),S));
   a.put("V_"+ax[i],rep(SumReport.class,"D_V_"+ax[i],uff("D_mVx"+i,"$D_mV*$$Position["+i+"]"),R));
   a.put("U_"+ax[i],rep(SumReport.class,"D_U_"+ax[i],uff("D_mUx"+i,"$D_mU*$$Position["+i+"]"),R));
  }
  if(aw!=null){a.put("V_aw",rep(SumReport.class,"D_V_aw",uff("D_mVaw","$D_mV*$"+aw.getFunctionName()),R));
               a.put("U_aw",rep(SumReport.class,"D_U_aw",uff("D_mUaw","$D_mU*$"+aw.getFunctionName()),R));}
  Path csv=out.resolve("DIAG.csv");
  Files.writeString(csv,"step,t_s,Cp_min_strut,S_n,S_x,S_y,S_z,Cp_min_vol,V_n,V_x,V_y,V_z,V_alfa_wody,U_max,U_n,U_x,U_y,U_z,U_alfa_wody\n");
  int k=0;
  while(s.getSolution().getPhysicalTime()<tEnd-1e-9){
   s.getSimulationIterator().step(5);k++;
   if(k%5!=0)continue;
   double cs=v(cps),cv=v(cpV),um=v(uMax);
   double ts=Math.min(-1.0,0.5*cs),tv=Math.min(-1.0,0.5*cv),tu=Math.max(12.0,0.7*um);
   mS.setDefinition("($TF7_Cp < "+ts+") ? 1 : 0");mV.setDefinition("($TF7_Cp < "+tv+") ? 1 : 0");mU.setDefinition("($D_speed > "+tu+") ? 1 : 0");
   StringBuilder b=new StringBuilder();b.append(s.getSimulationIterator().getCurrentIteration()).append(",").append(s.getSolution().getPhysicalTime());
   double n;
   b.append(",").append(cs);n=v(a.get("S_n"));b.append(",").append(n);for(String x:ax)b.append(",").append(n>0?v(a.get("S_"+x))/n:Double.NaN);
   b.append(",").append(cv);n=v(a.get("V_n"));b.append(",").append(n);for(String x:ax)b.append(",").append(n>0?v(a.get("V_"+x))/n:Double.NaN);
   b.append(",").append(a.containsKey("V_aw")&&n>0?v(a.get("V_aw"))/n:Double.NaN);
   b.append(",").append(um);n=v(a.get("U_n"));b.append(",").append(n);for(String x:ax)b.append(",").append(n>0?v(a.get("U_"+x))/n:Double.NaN);
   b.append(",").append(a.containsKey("U_aw")&&n>0?v(a.get("U_aw"))/n:Double.NaN);
   Files.writeString(csv,b.append("\n").toString(),StandardOpenOption.APPEND);
  }
  s.saveState(out.resolve("Y04_DIAG.sim").toString());
  p("koniec t="+s.getSolution().getPhysicalTime());
 }
}
