package macro;
import star.common.*;import java.lang.reflect.*;import java.nio.file.*;
public class ListHist extends StarMacro {
 void dump(StringBuilder b,String cn){try{Class<?> c=Class.forName(cn);for(Method m:c.getMethods())if(m.getDeclaringClass()!=Object.class&&!m.getName().matches("(get|set)(PresentationName|Comment|Tag|.*Listener.*)|.*journal.*|notify.*|wait|equals|hashCode"))b.append(c.getSimpleName()).append(": ").append(m.toString().replace("public ","")).append("\n");}catch(Throwable e){b.append(cn+" "+e+"\n");}}
 public void execute(){
  StringBuilder b=new StringBuilder();
  for(String cn:new String[]{"star.post.SolutionHistory","star.post.SolutionHistoryManager","star.post.RecordedSolutionView","star.post.SolutionViewManager","star.common.SolutionView"})dump(b,cn);
  try{Files.writeString(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"HIST_API.txt"),b.toString());}catch(Exception e){}
 }
}
