package org.test.utils;

import org.junit.Test;
import org.test.utils.more.MoreUtils;

import static io.github.gmazzo.codeowners.CodeOwners.getCodeOwners;
import static org.junit.Assert.assertEquals;
import static kotlin.collections.SetsKt.setOf;

public class UtilsOwnersTest {

    @Test
    public void ownerOfUtils() {
        assertEquals(setOf("utils-devs"), getCodeOwners(Utils.class));
    }

    @Test
    public void ownerOfMoreUtils() {
        assertEquals(setOf("utils-devs"), getCodeOwners(MoreUtils.class));
    }

}
