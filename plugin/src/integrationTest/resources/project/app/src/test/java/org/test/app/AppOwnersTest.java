package org.test.app;

import org.junit.Test;
import org.test.lib.LibClass;
import org.test.utils.AppUtils;
import org.test.utils.LibUtils;

import static com.github.gmazzo.codeowners.CodeOwners.getCodeOwners;
import static org.junit.Assert.assertEquals;
import static kotlin.collections.SetsKt.setOf;

public class AppOwnersTest {

    @Test
    public void ownerOfApp() {
        assertEquals(setOf("android-devs"), getCodeOwners(AppClass.class));
    }

    @Test
    public void ownerOfAppUtils() {
        assertEquals(setOf("kotlin-devs", "android-devs"), getCodeOwners(AppUtils.class));
    }

    @Test
    public void ownerOfLib() {
        assertEquals(setOf("kotlin-devs"), getCodeOwners(LibClass.class));
    }

    @Test
    public void ownerOfLibUtils() {
        assertEquals(setOf("kotlin-devs", "android-devs"), getCodeOwners(LibUtils.class));
    }

}
