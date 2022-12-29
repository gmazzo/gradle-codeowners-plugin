package org.test.app;

import org.junit.Test;
import org.test.lib.LibClass;
import org.test.utils.AppUtils;
import org.test.utils.LibUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.github.gmazzo.codeowners.CodeOwners.getCodeOwners;
import static java.util.Collections.singletonList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class AppOwnersTest {

    @Test
    public void ownerOfApp() {
        assertEquals(singletonList("android-devs"), getCodeOwners(AppClass.class));
    }

    @Test
    public void ownerOfLib() {
        assertEquals(singletonList("kotlin-devs"), getCodeOwners(LibClass.class));
    }

    @Test
    public void ownerOfUtils() {
        assertEquals(asList("android-devs", "kotlin-devs"), getCodeOwners(AppUtils.class));
        assertEquals(asList("android-devs", "kotlin-devs"), getCodeOwners(LibUtils.class));
    }

}
