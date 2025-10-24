package org.aliebashy.svctts;

//import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod("svctts")
@Mod.EventBusSubscriber(modid = "svctts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SVCTTS {
    public SVCTTS(){
        System.out.println("[SVCTTS] Mod Initialized.");
    }

    @SubscribeEvent
    public static void onChatMessage(ClientChatEvent event) {
    // Is the event called?
    System.out.println("Message Identified");

    
    // Intercept and modify chat messages
    String message = event.getMessage();    
    event.setMessage("[Intercepted] " + message);

    }

}