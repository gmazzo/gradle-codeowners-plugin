package org.test.utils;

import org.junit.Test;
import org.test.utils.more.MoreUtils;

import static io.github.gmazzo.codeowners.CodeOwnersUtils.getCodeOwners;
import static kotlin.collections.SetsKt.setOf;
import static org.junit.Assert.assertEquals;

public class UtilsOwnersTest {

    @Test
    public void ownerOfUtils() {
        assertEquals(setOf("utils-devs"), getCodeOwners(Utils.class));
    }

    @Test
    public void ownerOfMoreUtils() {
        assertEquals(setOf("utils-devs"), getCodeOwners(MoreUtils.class));
        assertEquals(setOf("utils-devs"), getCodeOwners(MoreUtils.Companion.class));
    }

}
