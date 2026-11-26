package macro;
import star.common.*;import star.base.neo.*;import star.base.report.*;import star.motion.*;import star.vis.*;import star.post.*;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;

/**
 * TFoilRampBack - przygotowanie rampy powrotnej (histereza wentylacji) z gotowego stanu rampy 0->45 (np. Y04 TF7_FINAL.sim przy t = 3,5 s, 45 deg).
 * Natywnie w STAR (edytowalne w GUI): ta sama RotatingMotion ("TF7 skret strutu ..."), nowa definicja predkosci obrotu:
 *   omega = -(y_from - y_to)/T  dla back_t0_s < t < back_t0_s + T, potem 0;  raport TF7_skret_deg = y_from - (y_from-y_to)*clamp((t-t0)/T).
 * Animacje scen TF7 -> <przypadek>\ANIMACJE, nowa historia <name>_historia.simh, kryterium czasu = t_end_s.
 * control.properties: name, back_from_deg (45), back_to_deg (0), back_t0_s (3.5), back_T_s (3.0), t_end_s, history_every.
 * Wynik: <name>_SETUP.sim + SETUP.json (stage=done) -> kontroler.py liczy dalej TFoilYawRun.java od t = back_t0_s.
 */
public class TFoilRampBack extends StarMacro {
 Simulation s; Path out; Properties cfg=new Properties(); Map<String,Object> log=new LinkedHashMap<>(); List<String> warn=new ArrayList<>();
 double d(String k,double v){return Double.parseDouble(cfg.getProperty(k,""+v));}
 Object call(Object o,String n,Object... a){for(Method mt:o.getClass().getMethods()){if(!mt.getName().equals(n)||mt.getParameterCount()!=a.length)continue;try{return mt.invoke(o,a);}catch(Throwable e){}}throw new IllegalStateException(n);}
 FieldFunction ffPres(String p){for(FieldFunction f:s.getFieldFunctionManager().getObjects())if(f.getPresentationName().equals(p))return f;return null;}
 void write(){try{StringBuilder b=new StringBuilder("{");boolean f=true;for(Map.Entry<String,Object> e:log.entrySet()){if(!f)b.append(",");f=false;
   b.append("\"").append(e.getKey()).append("\":\"").append(String.valueOf(e.getValue()).replace("\\","/").replace("\"","'")).append("\"");}
   b.append(",\"warnings\":\"").append(String.join(" | ",warn).replace("\\","/").replace("\"","'")).append("\"}");
   Files.writeString(out.resolve("SETUP.json"),b.toString());}catch(Exception e){}}
 public void execute(){
  s=getActiveSimulation();out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));
  try{
   try(java.io.Reader r=Files.newBufferedReader(out.resolve("control.properties"))){cfg.load(r);}
   String name=cfg.getProperty("name","YB");
   double y0=d("back_from_deg",45),y1=d("back_to_deg",0),t0=d("back_t0_s",3.5),T=d("back_T_s",3.0),tEnd=d("t_end_s",t0+T+0.1);
   double tNow=s.getSolution().getPhysicalTime();log.put("t_start",tNow);
   if(Math.abs(tNow-t0)>0.05)warn.add("czas startu "+tNow+" != back_t0_s "+t0);
   // 1. predkosc obrotu (ta sama RotatingMotion)
   RotatingMotion rm=null;for(Object o:s.get(MotionManager.class).getObjects())if(o instanceof RotatingMotion)rm=(RotatingMotion)o;
   if(rm==null)throw new IllegalStateException("brak RotatingMotion");
   double w=-Math.toRadians(y0-y1)/T;
   String om="($Time > <"+t0+" s> && $Time < <"+(t0+T)+" s>) ? <"+w+" radian/s> : <0 radian/s>";
   ScalarPhysicalQuantity q=((RotationRate)rm.getRotationSpecification()).getRotationRate();q.setDefinition(om);
   if(!om.equals(q.getDefinition()))throw new IllegalStateException("predkosc obrotu nie ustawiona: "+q.getDefinition());
   log.put("ramp_rate_check",q.getDefinition());log.put("ramp","skret "+y0+" -> "+y1+" deg, t "+t0+" .. "+(t0+T)+" s, potem staly do "+tEnd+" s");
   // 2. raport kata
   ExpressionReport ang=(ExpressionReport)s.getReportManager().getReport("TF7_skret_deg");
   ang.setDefinition(y0+"-"+(y0-y1)+"*min(max(($Time-<"+t0+" s>)/<"+T+" s>,0),1)");log.put("angle_report",ang.getDefinition());
   // 3. kryterium czasu
   for(SolverStoppingCriterion c:s.getSolverStoppingCriterionManager().getObjects())if(c instanceof PhysicalTimeStoppingCriterion){((PhysicalTimeStoppingCriterion)c).getMaximumTime().setValue(tEnd);c.setIsUsed(true);}
   log.put("t_end",tEnd);
   // 4. animacje -> ten przypadek
   Path anim=out.resolve("ANIMACJE");Files.createDirectories(anim);List<String> an=new ArrayList<>();
   for(Scene sc:s.getSceneManager().getScenes()){if(!sc.getPresentationName().startsWith("TF7"))continue;
    try{Object su=call(sc,"getSceneUpdate");String old=String.valueOf(call(su,"getAnimationFilePath"));
     String sub=Paths.get(old).getFileName().toString();Path d=anim.resolve(sub);Files.createDirectories(d);
     call(su,"setAnimationFilePath",d.toString());call(su,"setSaveAnimation",true);an.add(sub);}catch(Throwable e){warn.add("anim "+sc.getPresentationName()+": "+e);}}
   log.put("animations",an);
   // 5. nowa historia
   SolutionHistoryManager hm=s.get(SolutionHistoryManager.class);
   for(Object o:new ArrayList<>(hm.getObjects()))try{call(hm,"removeObjects",new NeoObjectVector(new Object[]{o}));}catch(Throwable e){warn.add("hist del: "+e);}
   try{Region reg=s.getRegionManager().getRegion("Subtract");
    String path=out.resolve(name+"_historia.simh").toAbsolutePath().toString().replace('\\','/');
    SolutionHistory sh=hm.createForFile(path,false);sh.setPresentationName("TF7 historia rampy powrotnej");
    try{call(sh.getInputs(),"setQuery",new Object[]{null});}catch(Throwable e){}
    sh.getInputs().setObjects(reg);sh.getRegions().setQuery(null);sh.getRegions().setObjects(reg);
    List<Object> fl=new ArrayList<>();for(String n:new String[]{"VolumeFractionair","VolumeFractionwater","Velocity","Pressure"})fl.add(s.getFieldFunctionManager().getFunction(n));
    FieldFunction cp=ffPres("TF7_Cp");if(cp!=null)fl.add(cp);FieldFunction qf=ffPres("Q-Criterion");if(qf!=null)fl.add(qf);
    sh.setFunctions(new NeoObjectVector(fl.toArray()));
    sh.getUpdate().getUpdateModeOption().setSelected(StarUpdateModeOption.Type.TIMESTEP);
    sh.getUpdate().getTimeStepUpdateFrequency().setTimeSteps(Integer.parseInt(cfg.getProperty("history_every","150")));
    log.put("history_path",path);}catch(Throwable e){warn.add("history: "+e);}
   log.put("stage","done");log.put("saved",name+"_SETUP.sim");write();
   s.saveState(out.resolve(name+"_SETUP.sim").toString());
  }catch(Throwable e){log.put("stage","error");log.put("error",String.valueOf(e));write();throw new RuntimeException(e);}
 }
}
