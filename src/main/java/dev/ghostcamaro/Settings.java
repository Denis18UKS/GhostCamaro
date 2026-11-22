package dev.ghostcamaro;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.*;
public final class Settings {
    public double maxSpeed=.88, acceleration=.009, normalBrake=.00520833, emergencyBrake=.01420455;
    public float standardGlassStep=6,reinforcedGlassStep=12,laminatedGlassStep=20;
    public float pistolDamage=5,rifleDamage=8;
    public float tank=60, idleConsumption=.0004f, driveConsumption=.003f, collisionDamage=65;
    public static <T> T load(String file, T defaults, Class<T> type) {
        Path path=FabricLoader.getInstance().getConfigDir().resolve(file);
        Gson gson=new GsonBuilder().setPrettyPrinting().create();
        try {
            if(Files.exists(path)) try(Reader in=Files.newBufferedReader(path)){T v=gson.fromJson(in,type);return v==null?defaults:v;}
            Files.writeString(path,gson.toJson(defaults));
        } catch(IOException|JsonParseException e){System.err.println("Ghost Camaro config: "+e);}
        return defaults;
    }
}
