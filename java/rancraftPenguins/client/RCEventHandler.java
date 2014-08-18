package rancraftPenguins.client;

import rancraftPenguins.RanCraftPenguins;
import rancraftPenguins.network.RCMessage1;
//import rancraftPenguins.network.RCMessage2;
import ibxm.Player;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RCEventHandler
{
    static int tickCount = 0; // static to preserve their values between calls
    static int tick2Count = 0; // this one only ticks once each time the data are checked
    static int prevAir = 0;

/*    @SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) // requires FMLCommonHandler.instance().bus()
	{
        //System.out.println(String.format("Looks like %s has logged in", event.player.getDisplayName()));
        event.player.addChatMessage(new ChatComponentText("Welcome to Rancraft Penguins, " + event.player.getDisplayName() + "!"));
	}
*/	
/*	@SubscribeEvent
	public void onRightClickEvent(PlayerInteractEvent event) // requires FMLCommonHandler.instance().bus()
	{
        System.out.println(String.format("Right-clicked on something"));
	}
*/
    
	@SubscribeEvent
	public void onLivingUpdateEvent(LivingUpdateEvent event) // requires MinecraftForge.EVENT_BUS
	{
		if(event.entity instanceof EntityPlayer)
		{
	    	Minecraft theMinecraft = Minecraft.getMinecraft();
	        int maxAir = 300; /* in case this changes in later versions of MC */
	        int curAir;
	        float fallDist;
	        int tickFreq = 5; /* increase this to avoid using too many cycles */
        	int depleteFreq = 5; // 4
	        EntityPlayerSP entityplayersp;
	        InventoryPlayer inventoryplayer;
	
	        if(true){ //mcc.inGameHasFocus){
		        entityplayersp = theMinecraft.thePlayer;
		        inventoryplayer = entityplayersp.inventory;
		
		        /* for various infrequent things */
		        tickCount--;
		        if(tickCount < 1){
		        	tickCount = 200; /* to avoid letting tickCount get too high */
		        }
		    	if(tickCount%tickFreq == 0){ // to avoid using too many cycles
		    		tick2Count--; // so this increments every tickFreq ticks
		            if(tick2Count < 1){
		            	tick2Count = 200; /* to avoid letting tick2Count get too high */
		            }
		
	            	//System.out.printf("Client: Checking if we're holding a penguin fishing rod: %s.\n", inventoryplayer.getCurrentItem().getDisplayName());
			        if(inventoryplayer.getCurrentItem() != null && inventoryplayer.getCurrentItem().getItem().equals(RanCraftPenguins.PenguinFishingRod)){
		            	//System.out.printf("Client: holding fishing rod. Requesting state from server.\n");
			        	sendFishingRodStateRequest();
			        }
		
			        if(inventoryplayer.armorItemInSlot(0) != null && inventoryplayer.armorItemInSlot(0).getItem().equals(RanCraftPenguins.PenguinBootsCloud)){
			        	if(!entityplayersp.isCollidedVertically){ // not touching ground
			        		//if(!entityplayersp.isCollidedVertically){
			        		fallDist = entityplayersp.fallDistance / 3.0F;
				        	sendFallDistanceInfo(fallDist); /* sending info to server */
			        	}
			        }
		
			        if(inventoryplayer.armorItemInSlot(0) != null && inventoryplayer.armorItemInSlot(0).getItem().equals(RanCraftPenguins.PenguinFlippers))
			        {
			            if(entityplayersp.isInWater()){ // faster in water
			            	//System.out.printf("Current velocity total=%f\n", Math.abs(entityplayersp.motionX) + Math.abs(entityplayersp.motionZ));
			            	if(Math.abs(entityplayersp.motionX) + Math.abs(entityplayersp.motionY) + Math.abs(entityplayersp.motionZ) < 1.5F){
					            entityplayersp.motionX *= 1.3F;
					            entityplayersp.motionY *= 1.1F;
					            entityplayersp.motionZ *= 1.3F;
			            	}
			            } else if(entityplayersp.onGround){ // much slower or slightly slower on land
				            if(inventoryplayer.armorItemInSlot(1) != null && inventoryplayer.armorItemInSlot(1).getItem().equals(RanCraftPenguins.PenguinPants)){
				            	// slightly slower if also wearing PenguinPants
					            entityplayersp.motionX *= 0.8F;
					            entityplayersp.motionZ *= 0.8F;
				            } else if(inventoryplayer.armorItemInSlot(1) == null || !(inventoryplayer.armorItemInSlot(1).getItem().equals(RanCraftPenguins.PenguinFlameLeggings))) {
				            	// much slower and can't jump if wearing no pants or something other than PenguinPants or PenguinFlameLeggings
					            entityplayersp.motionX *= 0.5F;
					            entityplayersp.motionZ *= 0.5F;
				            }
			            } else if(entityplayersp.movementInput.jump){ // can't jump with flippers unless either Penguin pants or leggings
			            	if(inventoryplayer.armorItemInSlot(1) == null || (!inventoryplayer.armorItemInSlot(1).getItem().equals(RanCraftPenguins.PenguinPants) && !inventoryplayer.armorItemInSlot(1).getItem().equals(RanCraftPenguins.PenguinFlameLeggings))) {
				            	entityplayersp.motionY *= 0.4D;
			            	} // else is wearing penguin pants or leggings, so can jump normally
			            }
			        }
			
			        /* Flame flippers increases swimming speed to double the speed of normal penguin flippers */
			        /* Also, less bouncing in the water, but harder to steer. Good for ocean crossings. */
			        /* FlamePenguin leggings eliminate the slowing effects of both types of flippers */
			        if(inventoryplayer.armorItemInSlot(0) != null && inventoryplayer.armorItemInSlot(0).getItem().equals(RanCraftPenguins.PenguinFlameFlippers))
			        {
			            if(entityplayersp.isInWater()){ // faster in water
				        	if(Math.abs(entityplayersp.motionX) + Math.abs(entityplayersp.motionZ) < 1.5F){
					            entityplayersp.motionX *= 1.7F;
					            entityplayersp.motionZ *= 1.7F;
				        	}
			            } else if(entityplayersp.onGround){ // slower on land unless PenguinFlameLeggings
				            if(inventoryplayer.armorItemInSlot(1) == null || !inventoryplayer.armorItemInSlot(1).getItem().equals(RanCraftPenguins.PenguinFlameLeggings)) {
					            entityplayersp.motionX *= 0.5F;
					            entityplayersp.motionZ *= 0.5F;
				            }
			            } else if(entityplayersp.movementInput.jump){ // can't jump unless PenguinFlameLeggings
				            if(inventoryplayer.armorItemInSlot(1) == null || !inventoryplayer.armorItemInSlot(1).getItem().equals(RanCraftPenguins.PenguinFlameLeggings)) {
				            	entityplayersp.motionY *= 0.5D;
				            }
			            }
			        }
			
			        /* Flame Penguin chest plate extinguishes the player once they're not in lava */
			        if(inventoryplayer.armorItemInSlot(2) != null && inventoryplayer.armorItemInSlot(2).getItem().equals(RanCraftPenguins.PenguinFlameChestPlate)){
			        	if(entityplayersp.isBurning()){
			        		entityplayersp.extinguish();
				        	//System.out.printf("Client: Player isBurning and wearing chestplate, so sending extinguish.\n");
			        		sendExtinguishMessage(entityplayersp);
			        	}
			        }
			        
			        /* Any penguin hat (quadruples air supply) */
			    	if(entityplayersp.isInsideOfMaterial(Material.water)){ // we're underwater
				        if(inventoryplayer.armorItemInSlot(3) != null && 
				        		((inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatEmp))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatKing))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatYE))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatMag))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatGal))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatWF))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatAdel))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatGent))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatHum))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatAfr))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatCS))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatMac))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatRH))
				        		  || (inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinHatLB)))) {
				        	// Usually quadruples time underwater, but sometimes much more -- air use stops for awhile
				        	curAir = entityplayersp.getAir();
				        	//depleteFreq = theMinecraft.theWorld.isRemote ? 23 : 4; // b/c air depletes faster in smp
		    	        	//System.out.printf("isRemote = %b\n", theMinecraft.theWorld.isRemote); // always true
		    	        	//System.out.printf("isClientSide = %b\n", (side == Side.CLIENT)); // always true
				        	if(tick2Count%depleteFreq == 0 || curAir < 1){ // check air and allow it to drop one in n ticks
			    	        	//System.out.printf("Client: Should I allow air to drop? curAir = %d, prevAir = %d\n", curAir, prevAir);
				        		if(prevAir > curAir){
				        			//curAir--; // decrement air (already being done at a higher level)
				        			prevAir = curAir;
				    	        	//System.out.printf("Client: ALLOWING air to drop: Airlevel = %d\n", curAir);
				        		}
				        	}
			        		entityplayersp.setAir(prevAir); // set air for this user to prevAir, which might be lower now
			        		sendAirLevelInfo(prevAir); /* sending info to server */
				        	//System.out.printf("client: JUST sent prevAir to server: %d: Deplete freq = %d\n", prevAir, depleteFreq);
				        /* Flame penguin mask (no air loss) */
				        } else if(inventoryplayer.armorItemInSlot(3) != null && inventoryplayer.armorItemInSlot(3).getItem().equals(RanCraftPenguins.PenguinFlameMask)) {
					    	curAir = entityplayersp.getAir();
					    	if(curAir < maxAir){ // fix it every time it goes below maximum
				        		entityplayersp.setAir(maxAir);
				        		sendAirLevelInfo(maxAir); /* sending info to server */
					        	//System.out.printf("client: JUST sent maxAir to server: %d: Deplete freq = %d\n", prevAir, depleteFreq);
			    	    	}
			        	} else {  // not wearing penguin mask or hat, so make sure prevAir is ready for hat-on calculations
			        		prevAir = maxAir;
				        	//System.out.printf("Client: Max air for prevAir for when hat goes back on: prevAir = %d\n", prevAir);
			        	}
			        } else { // not underwater
			    		prevAir = maxAir;
			        	curAir = maxAir;
			        	//System.out.printf("Client: Max air b/c not underwater! Airlevel = %d\n", prevAir);
			        }
		    	} // tickCount
			}
        }
	    return;
	}
	
	public void sendExtinguishMessage(EntityPlayerSP entityPlayerSp)
    {
    	try {
    		// Create Extinguish packet
    		RCMessage1 message = new RCMessage1("RC_Ext", 451);
    		// Send Extinguish packet
        	//System.out.printf("Client: Sending extinguish message...\n");
        	RanCraftPenguins.network1.sendToServer(message);
    	} catch (Exception ex) {
	        ex.printStackTrace();
            System.out.println("Client: Error creating packet");
    	}
    }

	//public void sendAirLevelInfo(EntityPlayerSP entityPlayerSp, int airLevel)
	public void sendAirLevelInfo(int airLevel)
    {
    	try {
    		// Create packet with Air Level info
    		RCMessage1 message = new RCMessage1("RC_AL", airLevel);
    		// Send Air Level packet
        	//System.out.printf("Client: Sending event= %s, airlevel=%d message\n", message.);
        	RanCraftPenguins.network1.sendToServer(message);
    	} catch (Exception ex) {
	        ex.printStackTrace();
            System.out.println("Client: Error creating packet");
    	}
    }

	public void sendFallDistanceInfo(float fallDistance)
    {
    	try {
    		// Create packet with Fall Distance info
    		RCMessage1 message = new RCMessage1("RC_FD", fallDistance);
    		// Send Fall Distance packet
        	//System.out.printf("Client: Sending falldistance=%f message\n", fallDistance);
        	RanCraftPenguins.network1.sendToServer(message);
    	} catch (Exception ex) {
	        ex.printStackTrace();
            System.out.println("Client: Error creating packet");
    	}
    }

	public void sendFishingRodStateRequest()
    {
    	try {
    		// Create packet with Fishing Rod State Request
    		RCMessage1 message = new RCMessage1("RC_FRSR");
        	//System.out.printf("Client: Sending FishingRodStateRequest.\n");
        	RanCraftPenguins.network1.sendToServer(message);
    	} catch (Exception ex) {
	        ex.printStackTrace();
            System.out.println("Client: Error creating packet");
    	}
    }
}
