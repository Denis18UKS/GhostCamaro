package dev.ghostcamaro;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import java.util.*;

/** Local coordinates: X left, Z forward. Keep the vanilla AABB for broad-phase lookup. */
public final class CarHull {
    private CarHull(){}
    // front/rear trim, body and low glasshouse have different heights and widths.
    private static final double[][] PARTS={{-2.47,-1.68,.90,.80},{-1.68,-.77,.955,.89},{-.77,.61,.96,.85},{.61,1.54,.955,.84},{1.54,2.35,.92,.81},{2.35,2.71,.93,.37},{-1.48,-.72,.73,1.06},{-.72,.10,.71,1.29},{.10,.61,.72,1.10}};
    public static Box bounds(GhostCamaroEntity c){
        Vec3d center=c.getPos().add(c.offset(0,0,.12));
        double a=Math.toRadians(c.getYaw()),s=Math.abs(Math.sin(a)),k=Math.abs(Math.cos(a));
        double w=k*.96+s*2.59,d=s*.96+k*2.59;
        return new Box(center.x-w,c.getY(),center.z-d,center.x+w,c.getY()+1.29,center.z+d);
    }
    public static Box[] pieces(GhostCamaroEntity c){
        Box[] boxes=new Box[PARTS.length];double a=Math.toRadians(c.getYaw()),s=Math.abs(Math.sin(a)),k=Math.abs(Math.cos(a));
        for(int i=0;i<boxes.length;i++){
            double[] p=PARTS[i];double h=(p[1]-p[0])*.5,w=k*p[2]+s*h,d=s*p[2]+k*h;
            Vec3d mid=c.getPos().add(c.offset(0,0,(p[1]+p[0])*.5));
            boxes[i]=new Box(mid.x-w,mid.y+(i>=6?.83:0),mid.z-d,mid.x+w,mid.y+p[3],mid.z+d);
        }
        return boxes;
    }
    public static boolean clear(GhostCamaroEntity c){
        for(Box b:pieces(c))if(!c.getWorld().isSpaceEmpty(c,b))return false;
        return true;
    }
    public static Optional<Vec3d> select(Vec3d start,Vec3d end){
        Vec3d nearest=null;
        for(int i=0;i<PARTS.length;i++){
            double[] p=PARTS[i];Box b=new Box(-p[2]-.012,i>=6?.83:-.01,p[0]-.012,p[2]+.012,p[3]+.012,p[1]+.012);
            if(b.contains(start))return Optional.of(start);
            Vec3d hit=b.raycast(start,end).orElse(null);
            if(hit!=null&&(nearest==null||hit.squaredDistanceTo(start)<nearest.squaredDistanceTo(start)))nearest=hit;
        }
        return Optional.ofNullable(nearest);
    }
}
