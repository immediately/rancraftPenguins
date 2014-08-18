package rancraftPenguins.network;

/* From diesieben07's post here:
 * http://www.minecraftforge.net/forum/index.php?topic=20135.0
 * Each type of message needs its own class
 */

import rancraftPenguins.network.RCMessage2;
import rancraftPenguins.ItemPenguinFishingRod;
import rancraftPenguins.RanCraftPenguins;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class RCMessage1 implements IMessage {
	   
    private int iPayload;
    private float fPayload;
    private String sPayload;
    private String eventType;

    public RCMessage1() { 
    	this.eventType = "dummyEvent";
    	this.sPayload = "dummyString";
    	this.iPayload = -1;
    	this.fPayload = -1.0F;
    }

/*    public RCMessage(String evType, String stringP) { // not used at this point
        this.eventType = evType;
        this.sPayload = stringP;
        this.iPayload = 0;
        this.fPayload = 0.0F;
    }
*/
    public RCMessage1(String evType) {
        this.eventType = evType;
    	this.sPayload = "";
        this.iPayload = 0;
    	this.fPayload = 0.0F;
    }

    public RCMessage1(String evType, int intP) {
        this.eventType = evType;
    	this.sPayload = "";
        this.iPayload = intP;
    	this.fPayload = 0.0F;
    }

    public RCMessage1(String evType, float floatP) {
        this.eventType = evType;
    	this.sPayload = "";
    	this.iPayload = 0;
        this.fPayload = floatP;
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
		//System.out.printf("RCMessage: Just read this string for eventType: %s\n", this.eventType);
       
        if(this.eventType.compareTo("RC_AL") == 0){
        	iPayload = buf.readInt();
        	//System.out.printf("RCMessage: Just read this int for air level: %d\n", this.iPayload);
        }
        if(this.eventType.compareTo("RC_FD") == 0){
        	fPayload = buf.readFloat();
        	//System.out.printf("RCMessage: Just read this int for fall distance: %f\n", this.fPayload);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
//		System.out.printf("RCMessage: writing this to buf: [%s]\n", sPayload);
//        ByteBufUtils.writeUTF8String(buf, sPayload);

		String e = new String(this.eventType);

		//System.out.printf("RCPacket: Write the length of the eventname, which is %d\n", e.length());
        buf.writeShort(e.length());
        for (char c : e.toCharArray()){
    		//System.out.printf("RCPacket: Writing %c\n", c);
        	buf.writeChar(c);
        }

        /* That's all we need for RC_Ext or RC_FRSR because it's enough to know the event type */

        /* for RC_Air we need to encode an int -- either MAXAIR or current air level */
        if(this.eventType.compareTo("RC_AL") == 0){
        	buf.writeInt(iPayload);
    	}
        /* for RC_FD we need to encode a float */
        if(this.eventType.compareTo("RC_FD") == 0){
        	buf.writeFloat(fPayload);
    	}
    }
    
    public static class Handler implements IMessageHandler<RCMessage1, IMessage> {
        
        @Override
        public IMessage onMessage(RCMessage1 message, MessageContext ctx) {
        	EntityPlayerMP pMP = null;
        	try{
        		pMP = ctx.getServerHandler().playerEntity;
        	} catch(Exception ex) {
    	        ex.printStackTrace();
                System.out.println("RCMessage: Error reading message sender ID");
        		return null;
        	}
            //System.out.println(String.format("Received %s event message", message.eventType));

            if(message.eventType.compareTo("RC_Ext") == 0){
                //System.out.println(String.format("Got an RC_Ext event type. Trying to extinguish player."));
                pMP.extinguish();
        	}
            if(message.eventType.compareTo("RC_AL") == 0){
                //System.out.println(String.format("Received airlevel = %d from %s", message.iPayload, pMP.getDisplayName()));
                pMP.setAir(message.iPayload);
        	}
            if(message.eventType.compareTo("RC_FD") == 0){
                //System.out.println(String.format("Received falldistance = %f from %s", message.fPayload, pMP.getDisplayName()));
                pMP.fallDistance = message.fPayload;
        	}
            if(message.eventType.compareTo("RC_FRSR") == 0 && pMP.inventory.getCurrentItem() != null && pMP.inventory.getCurrentItem().getItem() == RanCraftPenguins.PenguinFishingRod){
                //System.out.println(String.format("Got an RC_FRSR event type. Trying to send a return message."));
        		pMP = ctx.getServerHandler().playerEntity;
        		rancraftPenguins.ItemPenguinFishingRod pfr = (rancraftPenguins.ItemPenguinFishingRod)pMP.inventory.getCurrentItem().getItem();
        		RCMessage2 reply = new RCMessage2("RC_PFRI", pfr.getIcon());
            	RanCraftPenguins.network2.sendTo(reply, pMP); // 2nd thing to try
                //return reply; // simply return the reply message (but this would return it on the same network1, and that causes a packet leak)
        	}

            return null;
        }
    }
}