package macro;
import star.common.*;import star.base.neo.*;
import java.nio.file.*;import java.util.*;
/** Eksport scian strutu (x, y, z, udzial powietrza, normalna) z TF7_FINAL.sim -> STRUT_SCIANA.csv; tabela zostaje w drzewie (TF7 tabela strut). */
public class TFoilWaterline extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();
  Path out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));
  Region reg=s.getRegionManager().getRegion("Subtract");
  Boundary strut=reg.getBoundaryManager().getBoundary("plat_przedni_Olek.ColoredFace2");
  UserFieldFunction ny=s.getFieldFunctionManager().createFieldFunction();ny.setPresentationName("TF7_normalna_y");ny.setFunctionName("TF7_normalna_y");
  ny.getTypeOption().setSelected(FieldFunctionTypeOption.Type.SCALAR);ny.setDefinition("$${Area}[1]/mag($${Area})");
  XyzInternalTable t=s.getTableManager().createTable(XyzInternalTable.class);t.setPresentationName("TF7 tabela strut");
  t.getParts().setObjects(strut);
  t.setFieldFunctions(List.of(s.getFieldFunctionManager().getFunction("VolumeFractionair"),ny,s.getFieldFunctionManager().getFunction("AbsolutePressure")));
  t.extract();t.export(out.resolve("STRUT_SCIANA.csv").toString(),",");
  try{Files.writeString(out.resolve("WATERLINE_DONE.txt"),"ok t="+s.getSolution().getPhysicalTime());}catch(Exception e){}
 }
}
