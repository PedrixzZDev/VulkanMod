package net.vulkanmod.vulkan.queue;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public class QueueFamilyIndices {

    public static int graphicsFamily = VK_QUEUE_FAMILY_IGNORED;
    public static int presentFamily = VK_QUEUE_FAMILY_IGNORED;
    public static int transferFamily = VK_QUEUE_FAMILY_IGNORED;

    public static boolean hasDedicatedTransferQueue = false;

    public static boolean findQueueFamilies(VkPhysicalDevice device) {

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            if (queueFamilyCount.get(0) == 1) {
                transferFamily = presentFamily = graphicsFamily = 0;
                Initializer.LOGGER.info("Found single queue family. All queues supported.");
                return true;
            }

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            int fallbackTransfer = VK_QUEUE_FAMILY_IGNORED;
            int fallbackCompute = VK_QUEUE_FAMILY_IGNORED;

            for (int i = 0; i < queueFamilies.capacity(); i++) {
                int queueFlags = queueFamilies.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsFamily = i;
                }

                IntBuffer presentSupport = stack.ints(VK_FALSE);
                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);
 
                if (presentSupport.get(0) == VK_TRUE) {
                    presentFamily = i;
                }

                if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0 && (queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0) {
                    transferFamily = i;
                }

                if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    if (fallbackTransfer == VK_QUEUE_FAMILY_IGNORED) {
                        fallbackTransfer = i;
                    }
                    if ((queueFlags & VK_QUEUE_COMPUTE_BIT) == 0) {
                        transferFamily = i;
                    }
                }

                if ((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    if (fallbackCompute == VK_QUEUE_FAMILY_IGNORED) {
                        fallbackCompute = i;
                    }
                    if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) == 0) {
                        transferFamily = i;
                    }
                }

                if (isComplete()) break;
            }

            if (transferFamily == VK_QUEUE_FAMILY_IGNORED) {
                if (fallbackTransfer != VK_QUEUE_FAMILY_IGNORED) {
                    transferFamily = fallbackTransfer;
                } else if (fallbackCompute != VK_QUEUE_FAMILY_IGNORED) {
                    transferFamily = fallbackCompute;
                } else {
                    transferFamily = graphicsFamily;
                }
            }

            if (presentFamily == VK_QUEUE_FAMILY_IGNORED) {
                if (fallbackCompute != VK_QUEUE_FAMILY_IGNORED) {
                    presentFamily = fallbackCompute;
                } else {
                    presentFamily = graphicsFamily;
                }
            }

            hasDedicatedTransferQueue = graphicsFamily != transferFamily;

            if (transferFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Failed to find queue family with transfer support");
            if (graphicsFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with graphics support.");
            if (presentFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with present support.");

            return isComplete();
        }
    }

    public static boolean isComplete() {
        return graphicsFamily != VK_QUEUE_FAMILY_IGNORED && presentFamily != VK_QUEUE_FAMILY_IGNORED
                && transferFamily != VK_QUEUE_FAMILY_IGNORED;
    }

    public static boolean isSuitable() {
        return graphicsFamily != VK_QUEUE_FAMILY_IGNORED && presentFamily != VK_QUEUE_FAMILY_IGNORED;
    }

    public static int[] unique() {
        return IntStream.of(graphicsFamily, presentFamily, transferFamily).distinct().toArray();
    }

    public static int[] array() {
        return new int[]{graphicsFamily, presentFamily};
    }
}
