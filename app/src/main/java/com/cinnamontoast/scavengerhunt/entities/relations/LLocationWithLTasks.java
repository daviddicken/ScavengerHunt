package com.cinnamontoast.scavengerhunt.entities.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.cinnamontoast.scavengerhunt.entities.LLocation;
import com.cinnamontoast.scavengerhunt.entities.LTask;

import java.util.List;

public class LLocationWithLTasks {
    @Embedded
    public LLocation lLocation;
    @Relation(parentColumn = "id", entityColumn = "lLocationId")
    public List<LTask> lTasks;
}
