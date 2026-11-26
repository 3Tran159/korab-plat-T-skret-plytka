package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;
import java.nio.file.*;import java.io.*;import java.util.*;

/**
 * TFoilYawRun - liczy przypadek TF7 (VOF + IDDES), z zapisem stanu i terminem.
 *  - co 'chunk' krokow: wiersz do TF7_HISTORIA.csv (wszystkie raporty TF7_*), RUN_STATUS.json
 *  - punkt wznowienia co 'ckpt_every' krokow: TF7_CKPT_A/B.sim naprzemiennie (wskaznik LATEST_CHECKPOINT.json) + eksport wykresow do WYKRESY\
 *  - plik STOP albo termin 'deadline' (yyyy-MM-ddTHH:mm, czas lokalny) -> zapis punktu wznowienia i koniec
 *  - koniec: TF7_FINAL.sim
 */
public class TFoilYawRun extends StarMacro {
 Simulation s; Path out; Properties cfg=new Properties(); int ckSlot=0;
 static String q(String x){return "\""+x.replace("\\","/").replace("\"","'")+"\"";}
 void write(String n,Map<String,Object> v){try{StringBuilder b=new StringBuilder("{");boolean f=true;for(Map.Entry<String,Object> e:v.entrySet()){if(!f)b.append(",");f=false;Object o=e.getValue();
   b.append(q(e.getKey())).append(":").append(o instanceof Number&&Double.isFinite(((Number)o).doubleValue())?o.toString():o instanceof Boolean?o.toString():q(String.valueOf(o)));}
   Path p=out.resolve(n),t=out.resolve(n+".tmp");Files.writeString(t,b.append("}").toString());Files.move(t,p,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){}}
 public void execute(){
  s=getActiveSimulation();
  out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR",System.getProperty("user.dir")));
  Map<String,Object> st=new LinkedHashMap<>();
  try{
   try(Reader r=Files.newBufferedReader(out.resolve("control.properties"))){cfg.load(r);}
   int chunk=Integer.parseInt(cfg.getProperty("chunk","5")), ckEvery=Integer.parseInt(cfg.getProperty("ckpt_every","250"));
   long deadline=cfg.getProperty("deadline")==null?Long.MAX_VALUE:java.time.LocalDateTime.parse(cfg.getProperty("deadline")).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
   if(System.getenv().getOrDefault("TF_RESUMED_FROM","").contains("CKPT_A"))ckSlot=1;
   boolean resumed=Boolean.parseBoolean(System.getenv().getOrDefault("TF_RESUMED","false"));
   s.getSimulationIterator().getAutoSave().setEnabled(false);
   if(!resumed&&!Boolean.parseBoolean(cfg.getProperty("no_init","false")))s.getSolution().initializeSolution();
   List<Report> reps=new ArrayList<>();for(Report r:s.getReportManager().getObjects())if(r.getPresentationName().startsWith("TF7_"))reps.add(r);
   Path csv=out.resolve("TF7_HISTORIA.csv");
   if(!Files.exists(csv)){StringBuilder h=new StringBuilder("step,time_s,wall_s");for(Report r:reps)h.append(",").append(r.getPresentationName());Files.writeString(csv,h.append("\n").toString());}
   double tEnd=Double.parseDouble(cfg.getProperty("t_end_s","2.0"));
   long t0=System.currentTimeMillis();int sinceCk=0;
   while(true){
    double t=s.getSolution().getPhysicalTime();
    if(t>=tEnd-1e-9)break;
    if(Files.exists(out.resolve("STOP"))||System.currentTimeMillis()>deadline){save("CKPT");plots();st.put("state",Files.exists(out.resolve("STOP"))?"STOPPED_BY_FILE":"STOPPED_DEADLINE");write("RUN_STATUS.json",st);return;}
    s.getSimulationIterator().step(chunk);sinceCk+=chunk;
    StringBuilder row=new StringBuilder();row.append(s.getSimulationIterator().getCurrentIteration()).append(",").append(s.getSolution().getPhysicalTime()).append(",").append((System.currentTimeMillis()-t0)/1000.0);
    for(Report r:reps){double v;try{v=r.getReportMonitorValue();}catch(Throwable e){v=Double.NaN;}row.append(",").append(v);
     if(r.getPresentationName().matches("TF7_(L_kg|D_total|Fy_total|went_strut_pct|went_foil_m2|Cp_min_strut|interfaceCFL|skret_deg)"))st.put(r.getPresentationName(),v);}
    Files.writeString(csv,row.append("\n").toString(),StandardOpenOption.APPEND);
    st.put("state","RUNNING");st.put("time_s",s.getSolution().getPhysicalTime());st.put("wall_s",(System.currentTimeMillis()-t0)/1000.0);
    write("RUN_STATUS.json",st);
    if(sinceCk>=ckEvery){save("CKPT");plots();sinceCk=0;}
   }
   save("TF7_FINAL.sim");plots();
   st.put("state","DONE");write("RUN_STATUS.json",st);
  }catch(Throwable e){
   st.put("state","ERROR");st.put("error",e.toString());write("RUN_STATUS.json",st);
   try{save("TF7_CHECKPOINT_ERR.sim");}catch(Throwable x){}
   throw new RuntimeException(e);
  }
 }
 void plots(){
  try{Path d=out.resolve("WYKRESY");Files.createDirectories(d);
   for(StarPlot p:s.getPlotManager().getObjects())if(p.getPresentationName().startsWith("TF7"))
    try{p.encode(d.resolve(p.getPresentationName().replaceAll("[^A-Za-z0-9_ ]","").replace(' ','_')+".png").toString(),"png",1600,900);}catch(Throwable e){}
  }catch(Throwable e){}
 }
 void save(String n){
  if(n.equals("CKPT")){String slot=(ckSlot++%2==0)?"TF7_CKPT_A.sim":"TF7_CKPT_B.sim";
   // 2026-11-22: punkty zapisu na E: (prosba koordynatora NTNU, notka claude/notka-korab-pliki-przeniesione-na-E.md: T: sie konczy);
   // ckpt_dir w control.properties nadpisuje; bez E: -> katalog przypadku jak dotad
   Path ck=out;
   try{String cd=cfg.getProperty("ckpt_dir");Path e=Paths.get(cd!=null?cd:"E:/KORAB_PLAT_T_SKRET7_20261118_przeniesione/30_PRZEBIEGI/"+out.toAbsolutePath().getFileName());
    if(cd!=null||Files.exists(Paths.get("E:/"))){Files.createDirectories(e);ck=e;}}catch(Throwable e){ck=out;}
   // 2026-11-22: na CPU (np>1) stary plik bywa zablokowany ("Could not remove target file") -> zapis pod nowa nazwa zamiast przerwania
   try{s.saveState(ck.resolve(slot).toString());}
   catch(Throwable e){slot="TF7_CKPT_t"+String.format(java.util.Locale.ROOT,"%.4f",s.getSolution().getPhysicalTime())+".sim";s.saveState(ck.resolve(slot).toString());}
   Map<String,Object> m=new LinkedHashMap<>();m.put("path",ck.resolve(slot).toString());m.put("time_s",s.getSolution().getPhysicalTime());write("LATEST_CHECKPOINT.json",m);}
  else s.saveState(out.resolve(n).toString());
 }
}
