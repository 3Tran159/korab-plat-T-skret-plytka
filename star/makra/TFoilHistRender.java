package macro;
import star.common.*;import star.base.neo.*;import star.vis.*;import star.post.*;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;

/**
 * TFoilHistRender - filmy z historii rozwiazania (bez liczenia): otwiera *_historia.simh przypisana do symulacji,
 * buduje sceny "H.." (natywne, zostaja w pliku *_RENDER.sim), dla kazdego zapisanego stanu ustawia widok nagrany i drukuje klatki.
 * Na kazdej klatce napis: czas i kat skretu. Klatki: <case>\FILMY_HIST\<scena>\kNNNN.png
 * control.properties: name, h_m, U, yaw_deg, ramp_to_deg, ramp_t0_s, ramp_T_s, render_every (co ktory stan, domyslnie 1)
 */
public class TFoilHistRender extends StarMacro {
 Simulation s; Path out; Properties cfg=new Properties(); List<String> log=new ArrayList<>();
 double ZQC=-0.0164,ZS,U; Representation rep;
 double d(String k,double v){return Double.parseDouble(cfg.getProperty(k,""+v));}
 FieldFunction ff(String n){return s.getFieldFunctionManager().getFunction(n);}
 FieldFunction ffPres(String p){for(FieldFunction f:s.getFieldFunctionManager().getObjects())if(f.getPresentationName().equals(p))return f;return null;}
 Object call(Object o,String name,Object... a){for(Method mt:o.getClass().getMethods()){if(!mt.getName().equals(name)||mt.getParameterCount()!=a.length)continue;try{return mt.invoke(o,a);}catch(Throwable e){}}throw new IllegalStateException(name);}
 void soft(String w,Runnable r){try{r.run();}catch(Throwable e){log.add(w+": "+e);}}
 Object iso(String name,Region reg,FieldFunction f,double v){
  for(Method mt:s.getPartManager().getClass().getMethods())if(mt.getName().equals("createIsoPart")&&mt.getParameterCount()==2&&mt.getParameterTypes()[0]==Vector.class){
   try{IsoPart ip=(IsoPart)mt.invoke(s.getPartManager(),new Vector<Object>(List.of(reg)),f);ip.setPresentationName(name);
    try{Class<?> ec=Class.forName("star.vis.IsoMode");for(Object c:ec.getEnumConstants())if(c.toString().toUpperCase().contains("SINGLE"))call(ip,"setMode",c);}catch(Throwable e){}
    ip.getSingleIsoValue().setValue(v);return ip;}catch(Throwable e){log.add("iso "+e);}}
  return null;
 }
 PlaneSection plane(String name,Region reg,double[] o,double[] n){PlaneSection p=s.getPartManager().createPlaneSection();p.setPresentationName(name);p.getInputParts().setObjects(reg);
  p.setOrigin(new DoubleVector(o));p.setOrientation(new DoubleVector(n));return p;}
 ScalarDisplayer sd(Scene sc,String nm,Collection<?> parts,FieldFunction f,double lo,double hi){ScalarDisplayer x=sc.getDisplayerManager().createScalarDisplayer(nm);
  x.getInputParts().setObjects((Collection)parts);x.setRepresentation(rep);x.getScalarDisplayQuantity().setFieldFunction(f);
  x.getScalarDisplayQuantity().setAutoRange(AutoRangeMode.NONE);x.getScalarDisplayQuantity().setRange(new DoubleVector(new double[]{lo,hi}));return x;}
 void geo(Scene sc,Collection<?> parts,double op){PartDisplayer g=sc.getDisplayerManager().createPartDisplayer("geom");g.getInputParts().setObjects((Collection)parts);g.setRepresentation(rep);try{g.setOpacity(op);}catch(Throwable e){}}
 void view(Scene sc,double[] f,double[] p,double[] up){sc.getCurrentView().setInput(new DoubleVector(f),new DoubleVector(p),new DoubleVector(up),0.4,0,s.getCoordinateSystemManager().getLabCoordinateSystem(),false);}
 Map<String,Scene> scenes=new LinkedHashMap<>();
 Scene scene(String key,String title){Scene sc=s.getSceneManager().createScene(title);sc.initializeAndWait();scenes.put(key,sc);return sc;}

 Object streams=null;
 // linie pradu: dwie linie posiewu na dwoch glebokosciach (20 i 50 mm pod lustrem): przed strutem (x=0,30) i z boku napływu (y=-0,12); calkowanie w obie strony
 Object makeStreams(Region reg){
  List<Object> seeds=new ArrayList<>();
  for(double dz:new double[]{0.02,0.05}){
   seeds.add(line("H posiew przed strutem "+Math.round(dz*1000)+" mm",reg,new double[]{0.30,-0.09,ZS-dz},new double[]{0.30,0.09,ZS-dz}));
   seeds.add(line("H posiew z boku "+Math.round(dz*1000)+" mm",reg,new double[]{0.02,-0.12,ZS-dz},new double[]{0.32,-0.12,ZS-dz}));}
  seeds.removeIf(Objects::isNull);
  if(seeds.isEmpty())return null;
  FieldFunction v=ff("Velocity");
  for(Method mt:s.getPartManager().getClass().getMethods())if(mt.getName().equals("createStreamPart")&&mt.getParameterCount()==6&&mt.getParameterTypes()[1]==List.class&&mt.getParameterTypes()[3]==int.class){
   try{Object sp=mt.invoke(s.getPartManager(),new NeoObjectVector(new Object[]{reg}),new NeoObjectVector(seeds.toArray()),v,1,20,0);call(sp,"setPresentationName","H linie pradu");log.add("stream ok");return sp;}catch(Throwable e){log.add("stream "+e.getCause());}}
  return null;
 }
 Object line(String nm,Region reg,double[] a,double[] b){
  for(Method mt:s.getPartManager().getClass().getMethods())if(mt.getName().equals("createLinePart")&&mt.getParameterCount()==4&&mt.getParameterTypes()[0]==Vector.class){
   try{Object lp=mt.invoke(s.getPartManager(),new NeoObjectVector(new Object[]{reg}),new DoubleVector(a),new DoubleVector(b),4);call(lp,"setPresentationName",nm);return lp;}catch(Throwable e){log.add("line "+e.getCause());}}
  return null;
 }
 void addStreams(Scene sc){
  if(streams==null)return;
  soft("stream displayer",()->{StreamDisplayer d=sc.getDisplayerManager().createStreamDisplayer("linie pradu");d.getInputParts().setObjects((Collection)List.of(streams));d.setRepresentation(rep);
   try{d.setWidth(0.0015);}catch(Throwable e){}
   try{Object q=call(d,"getScalarDisplayQuantity");call(q,"setFieldFunction",ff("Velocity").getMagnitudeFunction());call(q,"setAutoRange",AutoRangeMode.NONE);call(q,"setRange",new DoubleVector(new double[]{0,1.3*U}));}catch(Throwable e){}});
 }
 public void execute(){
  s=getActiveSimulation();out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));
  try{
   try(java.io.Reader r=Files.newBufferedReader(out.resolve("control.properties"))){cfg.load(r);}
   ZS=ZQC+d("h_m",0.07519);U=d("U",8.333);
   Region reg=s.getRegionManager().getRegion("Subtract");
   Boundary foil=reg.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace1"),strut=reg.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace2");
   Boundary plate=reg.getBoundaryManager().getBoundary("TF7 plytka");   // 2026-11-21: plytka antywentylacyjna (Y05+) w scenach
   List<Boundary> W=plate==null?List.of(foil,strut):List.of(foil,strut,plate);
   List<Boundary> SP=plate==null?List.of(strut):List.of(strut,plate);
   // historia: obiekt zapisany w symulacji (bez tworzenia nowego pliku)
   SolutionHistory sh=null;for(Object o:s.get(SolutionHistoryManager.class).getObjects())sh=(SolutionHistory)o;
   if(sh==null)throw new IllegalStateException("brak historii w symulacji");
   SolutionHistory H=sh;soft("autorecord",()->call(H,"setAutoRecord",false));
   sh.rescanFile();log.add("historia: "+sh.getPath()+" stany "+sh.getMinStateIndex()+".."+sh.getMaxStateIndex());
   RecordedSolutionView rsv=sh.createRecordedSolutionView();rsv.setPresentationName("H historia");rep=rsv.getRepresentation();
   for(Scene sc:new ArrayList<>(s.getSceneManager().getScenes()))soft("scene upd off",()->{Object su=call(sc,"getSceneUpdate");call(su,"setSaveAnimation",false);});
   FieldFunction air=ff("VolumeFractionair"),water=ff("VolumeFractionwater"),cp=ffPres("TF7_Cp"),vel=ff("Velocity").getMagnitudeFunction();
   FieldFunction elev=ffPres("TF7_wzniesienie_lustra");
   FieldFunction q=ffPres("Q-Criterion");
   Object fs=iso("H lustro (alfa 0,5)",reg,water,0.5);
   Object qi=q==null?null:iso("H wiry Q",reg,q,d("q_iso",2e4));
   streams=makeStreams(reg);
   double xs=0.15;
   // H01/H02: zblizenie na strut z obu stron, powietrze na scianach + polprzezroczyste lustro
   for(int sg:new int[]{1,-1}){Scene sc=scene("H0"+(sg>0?1:2)+"_strut_"+(sg>0?"plus_y":"minus_y"),"H0"+(sg>0?1:2)+" strut z bliska, powietrze na scianach, od "+(sg>0?"+y":"-y"));
    sd(sc,"powietrze",W,air,0,1);if(fs!=null){ScalarDisplayer w=sd(sc,"lustro",List.of(fs),elev,-0.06,0.06);try{w.setOpacity(0.3);}catch(Throwable e){}}
    addStreams(sc);view(sc,new double[]{xs,0,ZS-0.03},new double[]{xs+0.05,sg*0.45,ZS-0.01},new double[]{0,0,1});}
   // H03: widok z dolu - powietrze schodzace po strucie na plat
   {Scene sc=scene("H03_z_dolu","H03 widok z dolu na plat i strut, powietrze na scianach");sd(sc,"powietrze",W,air,0,1);
    if(fs!=null){ScalarDisplayer w=sd(sc,"lustro",List.of(fs),elev,-0.06,0.06);try{w.setOpacity(0.25);}catch(Throwable e){}}
    addStreams(sc);view(sc,new double[]{xs,0,ZS-0.05},new double[]{xs-0.25,-0.35,ZS-0.55},new double[]{0,0,1});}
   {Scene sc=scene("H13_linie_pradu_z_gory","H13 linie pradu 20 i 50 mm pod lustrem, widok z gory");geo(sc,W,1.0);addStreams(sc);
    if(fs!=null){ScalarDisplayer w=sd(sc,"lustro",List.of(fs),elev,-0.06,0.06);try{w.setOpacity(0.25);}catch(Throwable e){}}
    view(sc,new double[]{0.12,-0.05,ZS-0.03},new double[]{0.12,-0.05,ZS+0.9},new double[]{-1,0,0});}
   // H04-H06: przekroje poziome 10 / 30 / 55 mm pod lustrem: udzial powietrza (jak gleboko schodzi kieszen)
   // H14: przekroj na glebokosci plytki (20 mm) - kontur plytki i powietrze w jej plaszczyznie
   if(plate!=null){double dz=d("plate_dz",0.02);PlaneSection p=plane("H przekroj plytki "+Math.round(dz*1000)+" mm",reg,new double[]{0,0,ZS-dz},new double[]{0,0,1});
    Scene sc=scene("H14_przekroj_plytki_"+Math.round(dz*1000)+"mm","H14 przekroj "+Math.round(dz*1000)+" mm pod lustrem (plaszczyzna plytki) - powietrze");sd(sc,"powietrze",List.of(p),air,0,1);geo(sc,SP,1.0);
    view(sc,new double[]{0.05,0,ZS-dz},new double[]{0.05,0,ZS+0.7},new double[]{-1,0,0});}
   int k=4;for(double dz:new double[]{0.010,0.030,0.055}){PlaneSection p=plane("H przekroj "+Math.round(dz*1000)+" mm pod lustrem",reg,new double[]{0,0,ZS-dz},new double[]{0,0,1});
    Scene sc=scene("H0"+k+"_przekroj_"+Math.round(dz*1000)+"mm","H0"+k+" przekroj "+Math.round(dz*1000)+" mm pod lustrem - powietrze");sd(sc,"powietrze",List.of(p),air,0,1);geo(sc,SP,1.0);
    view(sc,new double[]{0.05,0,ZS-dz},new double[]{0.05,0,ZS+0.7},new double[]{-1,0,0});k++;}
   // H07: przekroj pionowy w poprzek strutu (x = srodek cieciwy): profil kieszeni powietrza w glab
   {PlaneSection p=plane("H przekroj pionowy x=0,15",reg,new double[]{xs,0,0},new double[]{1,0,0});
    Scene sc=scene("H07_przekroj_pionowy","H07 przekroj pionowy w poprzek strutu - powietrze");sd(sc,"powietrze",List.of(p),air,0,1);geo(sc,W,1.0);
    view(sc,new double[]{xs,0,ZS-0.04},new double[]{xs+0.8,0,ZS-0.04},new double[]{0,0,1});}
   // H08: przekroj pionowy wzdluz sladu za strutem (y = 0): fala i powietrze za strutem
   {PlaneSection p=plane("H przekroj pionowy y=0",reg,new double[]{0,0,0},new double[]{0,1,0});
    Scene sc=scene("H08_przekroj_wzdluz","H08 przekroj pionowy wzdluz (y=0) - powietrze");sd(sc,"powietrze",List.of(p),air,0,1);geo(sc,W,1.0);
    view(sc,new double[]{0.0,0,ZS-0.03},new double[]{0.0,-0.9,ZS-0.03},new double[]{0,0,1});}
   // H09: wiry Q z tylu-z dolu
   if(qi!=null){Scene sc=scene("H09_wiry","H09 wiry (Q) kolor predkosc");sd(sc,"Q",List.of(qi),vel,0,1.3*U);geo(sc,W,1.0);
    if(fs!=null){PartDisplayer w=sc.getDisplayerManager().createPartDisplayer("lustro");w.getInputParts().setObjects((Collection)List.of(fs));w.setRepresentation(rep);try{w.setOpacity(0.2);}catch(Throwable e){}}
    addStreams(sc);view(sc,new double[]{0.05,0,ZS-0.04},new double[]{-0.6,-0.5,ZS-0.25},new double[]{0,0,1});}
   // H10: lustro z gory, wzniesienie, blizej
   if(fs!=null){Scene sc=scene("H10_lustro_z_gory","H10 lustro z gory - wzniesienie");sd(sc,"lustro",List.of(fs),elev,-0.06,0.06);geo(sc,W,1.0);
    view(sc,new double[]{-0.15,0,ZS},new double[]{-0.15,0,ZS+1.6},new double[]{-1,0,0});}
   // H11: Cp na scianach od strony ssacej i cisnieniowej (dwa widoki w jednym filmie nie - osobno)
   for(int sg:new int[]{1,-1}){Scene sc=scene("H1"+(sg>0?1:2)+"_Cp_"+(sg>0?"plus_y":"minus_y"),"H1"+(sg>0?1:2)+" Cp na scianach od "+(sg>0?"+y":"-y"));sd(sc,"Cp",W,cp,-1.5,0.8);
    view(sc,new double[]{xs,0,ZS-0.04},new double[]{xs,sg*0.6,ZS+0.1},new double[]{0,0,1});}
   // napis czas / kat na kazdej scenie
   Object ann=null;try{ann=call(s.getAnnotationManager(),"createSimpleAnnotation");call(ann,"setPresentationName","H czas i kat");
    for(Scene sc:scenes.values()){Object pm=call(sc,"getAnnotationPropManager");call(pm,"createPropForAnnotation",ann);}}catch(Throwable e){log.add("annotation: "+e);ann=null;}
   int every=(int)d("render_every",1);double y0=d("yaw_deg",0),y1=d("ramp_to_deg",45),t0=d("ramp_t0_s",0.4),T=d("ramp_T_s",3.0);
   Path dir=out.resolve("FILMY_HIST");int nfr=0;
   int off=(int)d("render_offset",0);   // rownolegle zadania: render_every=N, render_offset=0..N-1; klatki nazwane numerem stanu
   for(int i=sh.getMinStateIndex()+off;i<=sh.getMaxStateIndex();i+=every){
    rsv.setStateIndex(i);double t=rsv.getPhysicalTime();double ang=y0+(y1-y0)*Math.min(Math.max((t-t0)/T,0),1);
    if(ann!=null){final Object a=ann;soft("ann text",()->call(a,"setText",String.format(Locale.ROOT,"t = %.2f s   skret = %.1f deg   30 km/h, h = 75 mm",t,ang)));}
    for(Map.Entry<String,Scene> e:scenes.entrySet()){Path dd=dir.resolve(e.getKey());Files.createDirectories(dd);
     e.getValue().printAndWait(dd.resolve(String.format("k%04d.png",i)).toString(),1,1600,900);}
    nfr++;Files.writeString(out.resolve("RENDER_STATUS.txt"),"klatka "+nfr+" stan "+i+"/"+sh.getMaxStateIndex()+" t="+t+"\n"+String.join("\n",log));
   }
   if(off==0)s.saveState(out.resolve(cfg.getProperty("name","TF7")+"_RENDER.sim").toString());
   Files.writeString(out.resolve("RENDER_STATUS.txt"),"DONE klatek "+nfr+"\n"+String.join("\n",log));
  }catch(Throwable e){try{Files.writeString(out.resolve("RENDER_STATUS.txt"),"ERROR "+e+"\n"+String.join("\n",log));}catch(Exception x){}throw new RuntimeException(e);}
 }
}
