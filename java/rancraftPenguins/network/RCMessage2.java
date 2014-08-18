package rancraftPenguins.network;

/* From diesieben07's post here:
 * http://www.minecraftforge.net/forum/index.php?topic=20135.0
 * Each type of message needs its own class
 */

import rancraftPenguins.ItemPenguinFishingRod;
import rancraftPenguins.RanCraftPenguins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class RCMessage2 implements IMessage {
	   
    public int iPayload;
    private float fPayload;
    private String sPayload;
    public String eventType;

    public RCMessage2() { 
    	this.eventType = "dummyEvent";
    	this.sPayload = "dummyString";
    	this.iPayload = -1;
    	this.fPayload = -1.0F;
    }

    public RCMessage2(String evType, int intP) {
        this.eventType = evType;
    	this.sPayload = "";
        this.iPayload = intP;
    	this.fPayload = 0.0F;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        //sPayload = ByteBufUtils.readUTF8String(buf); // this class is very useful in general for writing more complex objects
		//System.out.printf("RCMessage: Just got this string from an incoming: [%s]\n", sPayload);
    	
    	// get length of eventType
        short len = buf.readShort();
        char[] pChars1 = new char[len];
        for (int i = 0; i < len; i++)
        	pChars1[i] = buf.readChar();
        this.eventType = String.valueOf(pChars1);
		//System.out.printf("RCMessage2: Just read this string for eventType: %s\n", this.eventType);
       
        if(this.eventType.compareTo("RC_PFRI") == 0){
        	iPayload = buf.readInt();
        	//System.out.printf("RCMessage2: Just read this int for fishing rod icon: %d\n", this.iPayload);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {

    	String e = new String(this.eventType);

		//System.out.printf("RCPacket: Write the length of the eventname, which is %d\n", e.length());
        buf.writeShort(e.length());
        for (char c : e.toCharArray()){
    		//System.out.printf("RCPacket: Writing %c\n", c);
        	buf.writeChar(c);
        }
        /* That's all we need for RC_Ext or RC_FRSR because it's enough to know the event type */

        /* for RC_PFRI we need to encode an int: 1 for cast state and 0 for not cast */
        if(this.eventType.compareTo("RC_PFRI") == 0){
    		//System.out.printf("RCMessage2: Writing fishing rod icon = %d to send to player\n", iPayload);
        	buf.writeInt(iPayload);
    	}
    }
    
    //@SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<RCMessage2, IMessage> {
        
        @Override
        public IMessage onMessage(RCMessage2 message, MessageContext ctx) {
        	//more effective than SideOnly(Side.CLIENT), b/c the client code and server code get completely separated this way
        	RanCraftPenguins.proxy.proxyOnMessage(message, ctx);

            return null; // no response in all other cases
        }
    }
}