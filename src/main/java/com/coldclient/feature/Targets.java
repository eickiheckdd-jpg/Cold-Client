package com.coldclient.feature;

import com.coldclient.module.FriendManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Shared target/weapon/input helpers for combat features. */
public final class Targets {
    private Targets() {}

    public static boolean validPlayer(LocalPlayer self, Entity e, boolean ignoreFriends) {
        if (!(e instanceof Player p) || p == self || !p.isAlive() || p.isSpectator()) {
            return false;
        }
        return !(ignoreFriends && FriendManager.isFriend(p));
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }

    public static boolean leftMouseDown() {
        long window = GLFW.glfwGetCurrentContext();
        return window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
