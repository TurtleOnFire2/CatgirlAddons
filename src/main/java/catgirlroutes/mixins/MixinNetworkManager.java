package catgirlroutes.mixins;

import catgirlroutes.events.impl.PacketSentEvent;
import catgirlroutes.events.impl.ReceiveChatPacketEvent;
import catgirlroutes.events.impl.TeleportEventPre;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import catgirlroutes.events.impl.PacketReceiveEvent;

@Mixin(value = {NetworkManager.class}, priority = 800)
public class MixinNetworkManager {
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        boolean shouldCancel = false;

        if (packet instanceof S08PacketPlayerPosLook) {
            if (MinecraftForge.EVENT_BUS.post(new TeleportEventPre((S08PacketPlayerPosLook) packet)))
                shouldCancel = true;
        }
        if (packet instanceof S02PacketChat) {
            MinecraftForge.EVENT_BUS.post(new ReceiveChatPacketEvent((S02PacketChat) packet));
        }
        if (MinecraftForge.EVENT_BUS.post(new PacketReceiveEvent(packet)))
            shouldCancel = true;
        if (shouldCancel)
            ci.cancel();
    }

    @Inject(method = {"sendPacket(Lnet/minecraft/network/Packet;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new PacketSentEvent(packet)))
            ci.cancel();
    }
}
