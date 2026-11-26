package macro;
import star.common.*;import star.base.neo.*;
import java.nio.file.*;import java.util.*;

/** Eksport tabeli komorek (x,y,z + jakosc siatki + Cp + |U| + alfa wody) do CELLS.csv w MINI_CASE_DIR. Bez liczenia. */
public class TFoilCellTable extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();Path out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));
  List<String> log=new ArrayList<>();
  try{
   Region reg=s.getRegionManager().getRegions().iterator().next();
   List<FieldFunction> fl=new ArrayList<>();
   for(String n:new String[]{"SkewnessAngle","CellQuality","VolumeChange","Volume","TF7_Cp","D_speed"}){FieldFunction f=s.getFieldFunctionManager().getFunction(n);if(f!=null)fl.add(f);else log.add("brak "+n);}
   for(FieldFunction f:s.getFieldFunctionManager().getObjects())if(f.getPresentationName().equals("Volume Fraction of water"))fl.add(f);
   XyzInternalTable t=s.getTableManager().createTable(XyzInternalTable.class);t.setPresentationName("TF7 komorki diag");
   t.setFieldFunctions(new NeoObjectVector(fl.toArray()));t.getParts().setObjects(reg);t.extract();
   t.export(out.resolve("CELLS.csv").toString(),",");log.add("ok "+fl.size()+" pol");
  }catch(Throwable e){log.add("ERROR "+e);}
  try{Files.write(out.resolve("CELLS.txt"),log);}catch(Exception e){}
 }
}
