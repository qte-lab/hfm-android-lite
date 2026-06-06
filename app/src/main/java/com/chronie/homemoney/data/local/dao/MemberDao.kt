package com.chronie.homemoney.data.local.dao

import androidx.room.*
import com.chronie.homemoney.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会员信息数据访问对象
 */
@Dao
interface MemberDao {
    
    @Query("SELECT * FROM members LIMIT 1")
    fun getCurrentMember(): Flow<MemberEntity?>
    
    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: String): MemberEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)
    
    @Update
    suspend fun updateMember(member: MemberEntity)
    
    @Delete
    suspend fun deleteMember(member: MemberEntity)
    
    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
}
