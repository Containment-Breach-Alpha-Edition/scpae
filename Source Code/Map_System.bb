Include "Source Code\Materials_System.bb"

Include "Source Code\Texture_Cache_System.bb"

Function LoadWorld(File$, rt.RoomTemplates)
	Local Map% = LoadAnimMesh_Strict(File)
	
	If Not Map Then Return
	
	Local x#, y#, z#, i%, c%
	Local mat.Materials
	Local World% = CreatePivot()
	Local Meshes% = CreatePivot(World)
	Local RenderBrushes% = CreateMesh(World)
	Local CollisionBrushes% = CreatePivot(World)
	
	EntityType(CollisionBrushes, HIT_MAP)
	
	For c = 1 To CountChildren(Map)
		Local Node% = GetChild(Map, c)	
		Local ClassName$ = Lower(KeyValue(Node, "classname"))
		
		Select ClassName
			Case "mesh"
				;[Block]
				EntityParent(Node, Meshes)
				
				If KeyValue(Node, "disablecollisions") <> 1 Then
					EntityType(Node, HIT_MAP)
					EntityPickMode(Node, 2)				
				EndIf
				
				c = c - 1
				;[End Block]
			Case "brush"
				;[Block]
				RotateMesh(Node, EntityPitch(Node), EntityYaw(Node), EntityRoll(Node))
				PositionMesh(Node, EntityX(Node), EntityY(Node), EntityZ(Node))
				
				AddMesh(Node, RenderBrushes)
				
				EntityAlpha(Node, 0.0)
				EntityType(Node, HIT_MAP)
				EntityAlpha(Node, 0.0)
				EntityParent(Node, CollisionBrushes)
				EntityPickMode(Node, 2)
				
				c = c - 1
				;[End Block]
			Case "screen"
				;[Block]
				x = EntityX(Node) * RoomScale
				y = EntityY(Node) * RoomScale
				z = EntityZ(Node) * RoomScale
				
				If x <> 0 Or y <> 0 Or z <> 0 Then 
					Local ts.TempScreens = New TempScreens	
					
					ts\x = x
					ts\y = y
					ts\z = z
					ts\ImgPath = KeyValue(Node, "imgpath", "")
					ts\roomtemplate = rt
				EndIf
				;[End Block]
			Case "waypoint"
				;[Block]
				x = EntityX(Node) * RoomScale
				y = EntityY(Node) * RoomScale
				z = EntityZ(Node) * RoomScale	
				
				Local w.TempWayPoints = New TempWayPoints
				
				w\roomtemplate = rt
				w\x = x
				w\y = y
				w\z = z
				;[End Block]
			Case "light"
				;[Block]
				x = EntityX(Node) * RoomScale
				y = EntityY(Node) * RoomScale
				z = EntityZ(Node) * RoomScale
				
				If x <> 0 Or y <> 0 Or z <> 0 Then 
					Range# = Float(KeyValue(Node, "range", "1")) / 2000.0
					lColor$ = KeyValue(Node, "color", "255 255 255")
					Intensity# = Min(Float(KeyValue(Node, "intensity", "1.0")) * 0.8, 1.0)
					R = Int(Piece(lColor, 1, " ")) * Intensity
					G = Int(Piece(lColor, 2, " ")) * Intensity
					B = Int(Piece(lColor, 3, " ")) * Intensity
					
					AddTempLight(rt, x, y, z, 2, Range, R, G, B)
				EndIf
				;[End Block]
			Case "spotlight"	
				;[Block]
				x = EntityX(Node) * RoomScale
				y = EntityY(Node) * RoomScale
				z = EntityZ(Node) * RoomScale
				If x <> 0 Or y <> 0 Or z <> 0 Then 
					Range# = Float(KeyValue(Node, "range", "1")) / 700.0
					lColor$ = KeyValue(Node, "color", "255 255 255")
					Intensity# = Min(Float(KeyValue(Node, "intensity", "1.0")) * 0.8, 1.0)
					R = Int(Piece(lColor, 1, " ")) * Intensity
					G = Int(Piece(lColor, 2, " ")) * Intensity
					B = Int(Piece(lColor, 3, " ")) * Intensity
					
					Local lt.LightTemplates = AddTempLight(rt, x, y, z, 3, Range, R, G, B)
					
					Angles$ = KeyValue(Node, "angles", "0 0 0")
					Pitch# = Piece(Angles, 1, " ")
					Yaw# = Piece(Angles, 2, " ")
					lt\Pitch = Pitch
					lt\Yaw = Yaw
					
					lt\InnerConeAngle = Int(KeyValue(Node, "innerconeangle", ""))
					lt\OuterConeAngle = Int(KeyValue(Node, "outerconeangle", ""))	
				EndIf
				;[End Block]
			Case "soundemitter"
				;[Block]
				For i = 0 To 3
					If rt\TempSoundEmitter[i] = 0 Then
						rt\TempSoundEmitterX[i] = EntityX(Node) * RoomScale
						rt\TempSoundEmitterY[i] = EntityY(Node) * RoomScale
						rt\TempSoundEmitterZ[i] = EntityZ(Node) * RoomScale
						rt\TempSoundEmitter[i] = Int(KeyValue(Node, "sound", "0"))
						rt\TempSoundEmitterRange[i] = Float(KeyValue(Node, "range", "1"))
						Exit
					EndIf
				Next
				;[End Block]
			; ~ Invisible collision brush
			Case "field_hit"
				;[Block]
				EntityParent(Node, CollisionBrushes)
				EntityType(Node, HIT_MAP)
				EntityAlpha(Node, 0.0)
				c = c - 1
				;[End Block]
			; ~ Camera start position point entity
			Case "playerstart"
				;[Block]
				Angles$ = KeyValue(Node, "angles", "0 0 0")
				Pitch# = Piece(Angles, 1, " ")
				Yaw# = Piece(Angles, 2, " ")
				Roll# = Piece(Angles, 3, " ")
				If Cam Then
					PositionEntity(Cam, EntityX(Node), EntityY(Node), EntityZ(Node))
					RotateEntity(Cam, Pitch, Yaw, Roll)
				EndIf
				;[End Block]
		End Select
	Next
	EntityFX(RenderBrushes, 1)
	FreeEntity(Map)
	Return(World)	
End Function

Function StripFilename$(File$)
	Local mi$ = ""
	Local LastSlash% = 0
	Local i%
	
	If Len(File) > 0
		For i = 1 To Len(File)
			mi = Mid(File, i, 1)
			If mi = "\" Or mi = "/" Then
				LastSlash = i
			EndIf
		Next
	EndIf
	Return(Left(File, LastSlash))
End Function

Function LoadRMesh(File$, rt.RoomTemplates)
	CatchErrors("Uncaught (LoadRMesh)")
	; ~ Generate a texture made of white
	Local BlankTexture%
	
	BlankTexture = CreateTexture(4, 4, 1, 1)
	ClsColor(255, 255, 255)
	SetBuffer(TextureBuffer(BlankTexture))
	Cls
	SetBuffer(BackBuffer())
	
	Local PinkTexture%
	
	PinkTexture = CreateTexture(4, 4, 1, 1)
	ClsColor(255, 255, 255)
	SetBuffer(TextureBuffer(PinkTexture))
	Cls
	SetBuffer(BackBuffer())
	
	ClsColor(0, 0, 0) 
	
	; ~ Read the file
	Local f% = ReadFile(File)
	Local i%, j%, k%, x#, y#, z#, Yaw#
	Local Vertex%
	Local Temp1i%, Temp2i%, Temp3i%
	Local Temp1#, Temp2#, Temp3#
	Local Temp1s$, Temp2s$
	Local CollisionMeshes% = CreatePivot()
	Local HasTriggerBox% = False
	
	For i = 0 To 3 ; ~ Reattempt up to 3 times
		If f = 0 Then
			f = ReadFile(File)
			Else
			Exit
		EndIf
	Next
	If f = 0 Then RuntimeError("Error reading file " + Chr(34) + File + Chr(34))
	
	Local IsRMesh$ = ReadString(f)
	
	If IsRMesh = "RoomMesh"
		;~ Continue
	ElseIf IsRMesh = "RoomMesh.HasTriggerBox"
		HasTriggerBox = True
	Else
		RuntimeError(Chr(34) + File + Chr(34) + " is Not RMESH (" + IsRMesh + ")")
	EndIf
	
	File = StripFilename(File)
	
	Local Count%, Count2%
	; ~ Drawn meshes
	Local Opaque%, Alpha%
	
	Opaque = CreateMesh()
	Alpha = CreateMesh()
	
	Count = ReadInt(f)
	
	Local ChildMesh%
	Local Surf%, Tex%[2], Brush%
	Local IsAlpha%
	Local u#, v#
	
	For i = 1 To Count ; ~ Drawn mesh
		ChildMesh = CreateMesh()
		
		Surf = CreateSurface(ChildMesh)
		
		Brush = CreateBrush()
		
		Tex[0] = 0 : Tex[1] = 0
		
		IsAlpha = 0
		For j = 0 To 1
			Temp1i = ReadByte(f)
			If Temp1i <> 0 Then
				Temp1s = ReadString(f)
				Tex[j] = GetTextureFromCache(Temp1s)
				If Tex[j] = 0 Then ; ~ Texture is not in cache
					Select True
						Case Temp1i < 3
							;[Block]
							Tex[j] = LoadTexture(File + Temp1s, 1)
							;[End Block]
						Default
							;[Block]
							Tex[j] = LoadTexture(File + Temp1s, 3)
							;[End Block]
					End Select
					
					If Tex[j] <> 0 Then
						If Temp1i = 1 Then TextureBlend(Tex[j], 5)
						If Instr(Lower(Temp1s), "_lm") <> 0 Then
							TextureBlend(Tex[j], 3)
						EndIf
						AddTextureToCache(Tex[j])
					EndIf
				EndIf
				If Tex[j] <> 0 Then
					IsAlpha = 2
					If Temp1i = 3 Then IsAlpha = 1
					
					TextureCoords(Tex[j], 1 - j)
				EndIf
			EndIf
		Next
		
		If IsAlpha = 1 Then
			If Tex[1] <> 0 Then
				TextureBlend(Tex[1], 2)
				BrushTexture(Brush, Tex[1], 0, 0)
			Else
				BrushTexture(Brush, BlankTexture, 0, 0)
			EndIf
		Else
			If Tex[0] <> 0 And Tex[1] <> 0 Then
				BumpTex% = GetBumpFromCache(StripPath(TextureName(Tex[1])))
				For j = 0 To 1
					BrushTexture(Brush, Tex[j], 0, j + 1 + (BumpTex <> 0))
				Next
				
				BrushTexture(Brush, AmbientLightRoomTex, 0)
				If (BumpTex <> 0) Then
					BrushTexture(Brush, BumpTex, 0, 1)
				EndIf
			Else
				For j = 0 To 1
					If Tex[j] <> 0 Then
						BrushTexture(Brush, Tex[j], 0, j)
					Else
						BrushTexture(Brush, BlankTexture, 0, j)
					EndIf
				Next
			EndIf
		EndIf
		
		Surf = CreateSurface(ChildMesh)
		
		If IsAlpha > 0 Then PaintSurface(Surf, Brush)
		
		FreeBrush(Brush) : Brush = 0
		
		Count2 = ReadInt(f) ; ~ Vertices
		
		For j = 1 To Count2
			; ~ World coordinatess
			x = ReadFloat(f) : y = ReadFloat(f) : z = ReadFloat(f)
			Vertex = AddVertex(Surf, x, y, z)
			
			; ~ Texture coords
			For k = 0 To 1
				u = ReadFloat(f) : v = ReadFloat(f)
				VertexTexCoords(Surf, Vertex, u, v, 0.0, k)
			Next
			
			; ~ Colors
			Temp1i = ReadByte(f)
			Temp2i = ReadByte(f)
			Temp3i = ReadByte(f)
			VertexColor(Surf, Vertex, Temp1i, Temp2i, Temp3i, 1.0)
		Next
		
		Count2 = ReadInt(f) ; ~ Polys
		For j = 1 To Count2
			Temp1i = ReadInt(f) : Temp2i = ReadInt(f) : Temp3i = ReadInt(f)
			AddTriangle(Surf, Temp1i, Temp2i, Temp3i)
		Next
		
		If IsAlpha = 1 Then
			AddMesh(ChildMesh, Alpha)
			EntityAlpha(ChildMesh, 0.0)
		Else
			AddMesh(ChildMesh, Opaque)
			EntityParent(ChildMesh, CollisionMeshes)
			EntityAlpha(ChildMesh, 0.0)
			EntityType(ChildMesh, HIT_MAP)
			EntityPickMode(ChildMesh, 2)
			
			; ~ Make collision double-sided
			Local FlipChild% = CopyMesh(ChildMesh)
			
			FlipMesh(FlipChild)
			AddMesh(FlipChild, ChildMesh)
			FreeEntity(FlipChild)			
		EndIf
	Next
	
	Local HiddenMesh%
	
	HiddenMesh = CreateMesh()
	
	Count = ReadInt(f) ; ~ Invisible collision mesh
	For i = 1 To Count
		Surf = CreateSurface(HiddenMesh)
		Count2 = ReadInt(f) ; ~ Vertices
		For j = 1 To Count2
			; ~ World coordinates
			x = ReadFloat(f) : y = ReadFloat(f) : z = ReadFloat(f)
			Vertex = AddVertex(Surf, x, y, z)
		Next
		
		Count2 = ReadInt(f) ; ~ Polys
		For j = 1 To Count2
			Temp1i = ReadInt(f) : Temp2i = ReadInt(f) : Temp3i = ReadInt(f)
			AddTriangle(Surf, Temp1i, Temp2i, Temp3i)
			AddTriangle(Surf, Temp1i, Temp3i, Temp2i)
		Next
	Next
	
	; ~ Trigger boxes
	If HasTriggerBox
		Local TB%
		
		rt\TempTriggerboxAmount = ReadInt(f)
		For TB = 0 To rt\TempTriggerboxAmount - 1
			rt\TempTriggerbox[TB] = CreateMesh(rt\OBJ)
			Count = ReadInt(f)
			For i = 1 To Count
				Surf = CreateSurface(rt\TempTriggerbox[TB])
				Count2 = ReadInt(f)
				For j = 1 To Count2
					x = ReadFloat(f) : y = ReadFloat(f) : z = ReadFloat(f)
					Vertex = AddVertex(Surf, x, y, z)
				Next
				Count2 = ReadInt(f)
				For j = 1 To Count2
					Temp1i = ReadInt(f) : Temp2i = ReadInt(f) : Temp3i = ReadInt(f)
					AddTriangle(Surf, Temp1i, Temp2i, Temp3i)
					AddTriangle(Surf, Temp1i, Temp3i, Temp2i)
				Next
			Next
			rt\TempTriggerboxName[TB] = ReadString(f)
		Next
	EndIf
	
	Count = ReadInt(f) ; ~ Point entities
	For i = 1 To Count
		Temp1s = ReadString(f)
		Select Temp1s
			Case "screen"
				;[Block]
				Temp1 = ReadFloat(f) * RoomScale
				Temp2 = ReadFloat(f) * RoomScale
				Temp3 = ReadFloat(f) * RoomScale
				
				Temp2s = ReadString(f)
				
				If Temp1 <> 0 Or Temp2 <> 0 Or Temp3 <> 0 Then 
					Local ts.TempScreens = New TempScreens
					
					ts\x = Temp1
					ts\y = Temp2
					ts\z = Temp3
					ts\ImgPath = Temp2s
					ts\roomtemplate = rt
				EndIf
				;[End Block]
			Case "waypoint"
				;[Block]
				Temp1 = ReadFloat(f) * RoomScale
				Temp2 = ReadFloat(f) * RoomScale
				Temp3 = ReadFloat(f) * RoomScale
				
				Local w.TempWayPoints = New TempWayPoints
				
				w\roomtemplate = rt
				w\x = Temp1
				w\y = Temp2
				w\z = Temp3
				;[End Block]
			Case "light"
				;[Block]
				Temp1 = ReadFloat(f) * RoomScale
				Temp2 = ReadFloat(f) * RoomScale
				Temp3 = ReadFloat(f) * RoomScale
				
				If Temp1 <> 0 Or Temp2 <> 0 Or Temp3 <> 0 Then 
					Range# = ReadFloat(f) / 2000.0
					lColor$ = ReadString(f)
					Intensity# = Min(ReadFloat(f) * 0.8, 1.0)
					R% = Int(Piece(lColor, 1, " ")) * Intensity
					G% = Int(Piece(lColor, 2, " ")) * Intensity
					B% = Int(Piece(lColor, 3, " ")) * Intensity
					
					AddTempLight(rt, Temp1, Temp2, Temp3, 2, Range, R, G, B)
				Else
					ReadFloat(f) : ReadString(f) : ReadFloat(f)
				EndIf
				;[End Block]
			Case "spotlight"
				;[Block]
				Temp1 = ReadFloat(f) * RoomScale
				Temp2 = ReadFloat(f) * RoomScale
				Temp3 = ReadFloat(f) * RoomScale
				
				If Temp1 <> 0 Or Temp2 <> 0 Or Temp3 <> 0 Then 
					Range# = ReadFloat(f) / 2000.0
					lColor$ = ReadString(f)
					Intensity# = Min(ReadFloat(f) * 0.8, 1.0)
					R% = Int(Piece(lColor, 1, " ")) * Intensity
					G% = Int(Piece(lColor, 2, " ")) * Intensity
					B% = Int(Piece(lColor, 3, " ")) * Intensity
					
					Local lt.LightTemplates = AddTempLight(rt, Temp1, Temp2, Temp3, 2, Range, R, G, B)
					
					Angles$ = ReadString(f)
					Pitch# = Piece(Angles, 1, " ")
					Yaw = Piece(Angles, 2, " ")
					lt\Pitch = Pitch
					lt\Yaw = Yaw
					
					lt\InnerConeAngle = ReadInt(f)
					lt\OuterConeAngle = ReadInt(f)
				Else
					ReadFloat(f) : ReadString(f) : ReadFloat(f) : ReadString(f) : ReadInt(f) : ReadInt(f)
				EndIf
				;[End Block]
			Case "soundemitter"
				;[Block]
				Temp1i = 0
				
				For j = 0 To MaxRoomEmitters - 1
					If rt\TempSoundEmitter[j] = 0 Then
						rt\TempSoundEmitterX[j] = ReadFloat(f) * RoomScale
						rt\TempSoundEmitterY[j] = ReadFloat(f) * RoomScale
						rt\TempSoundEmitterZ[j] = ReadFloat(f) * RoomScale
						
						rt\TempSoundEmitter[j] = ReadInt(f)
						
						rt\TempSoundEmitterRange[j] = ReadFloat(f)
						Temp1i = 1
						Exit
					EndIf
				Next
				
				If Temp1i = 0 Then
					ReadFloat(f)
					ReadFloat(f)
					ReadFloat(f)
					ReadInt(f)
					ReadFloat(f)
				EndIf
				;[End Block]
			Case "playerstart"
				;[Block]
				Temp1 = ReadFloat(f) : Temp2 = ReadFloat(f) : Temp3 = ReadFloat(f)
				
				Angles$ = ReadString(f)
				Pitch# = Piece(Angles, 1, " ")
				Yaw = Piece(Angles, 2, " ")
				Roll# = Piece(Angles, 3, " ")
				If cam Then
					PositionEntity(Cam, Temp1, Temp2, Temp3)
					RotateEntity(Cam, Pitch, Yaw, Roll)
				EndIf
				;[End Block]
			Case "model"
				;[Block]
				File = ReadString(f)
				If File <> ""
					Local Model% = CreatePropObj("GFX\Map\Props\" + File)
					
					Temp1 = ReadFloat(f) : Temp2 = ReadFloat(f) : Temp3 = ReadFloat(f)
					PositionEntity(Model, Temp1, Temp2, Temp3)
					
					Temp1 = ReadFloat(f) : Temp2 = ReadFloat(f) : Temp3 = ReadFloat(f)
					RotateEntity(Model, Temp1, Temp2, Temp3)
					
					Temp1 = ReadFloat(f) : Temp2 = ReadFloat(f) : Temp3 = ReadFloat(f)
					ScaleEntity(Model, Temp1, Temp2, Temp3)
					
					EntityParent(Model, Opaque)
					EntityType(Model, HIT_MAP)
					EntityPickMode(Model, 2)
				Else
					Temp1 = ReadFloat(f) : Temp2 = ReadFloat(f) : Temp3 = ReadFloat(f)
				EndIf
		End Select
	Next
	
	Local OBJ%
	
	Temp1i = CopyMesh(Alpha)
	FlipMesh(Temp1i)
	AddMesh(Temp1i, Alpha)
	FreeEntity(Temp1i)
	
	If Brush <> 0 Then FreeBrush(Brush)
	
	AddMesh(Alpha, Opaque)
	FreeEntity(Alpha)
	
	EntityFX(Opaque, 3)
	
	EntityAlpha(HiddenMesh, 0.0)
	EntityAlpha(Opaque, 1.0)
	
	EntityType(HiddenMesh, HIT_MAP)
	FreeTexture(BlankTexture)
	
	OBJ = CreatePivot()
	CreatePivot(OBJ) ; ~ Skip "meshes" object
	EntityParent(Opaque, OBJ)
	EntityParent(HiddenMesh, OBJ)
	CreatePivot(OBJ) ; ~ Skip "pointentites" object
	CreatePivot(OBJ) ; ~ Skip "solidentites" object
	EntityParent(CollisionMeshes, OBJ)
	
	CloseFile(f)
	
	CatchErrors("LoadRMesh")
	
	Return(OBJ)
End Function

Function StripPath$(File$) 
	Local Name$ = ""
	Local i%
	
	If Len(File) > 0 
		For i = Len(File) To 1 Step -1 
			mi$ = Mid(File, i, 1) 
			If mi$ = "\" Or mi$ = "/" Then Return(Name)
			
			Name = mi$ + Name 
		Next 
	EndIf 
	
	Return(Name) 
End Function

Function Piece$(s$, Entry%, Char$ = " ")
	While Instr(s, Char + Char)
		s = Replace(s, Char + Char, Char)
	Wend
	For n = 1 To Entry - 1
		p = Instr(s, Char)
		s = Right(s, Len(s) - p)
	Next
	p = Instr(s, Char)
	If p < 1
		a$ = s
	Else
		a = Left(s, p - 1)
	EndIf
	Return(a)
End Function

Function KeyValue$(Entity, Key$, DefaultValue$ = "")
	Properties$ = EntityName(Entity)
	Properties$ = Replace(Properties$, Chr(13), "")
	Key = Lower(Key)
	Repeat
		p = Instr(Properties, Chr(10))
		If p Then Test$ = (Left(Properties, p - 1)) Else Test = Properties
		TestLey$ = Piece(Test, 1, "=")
		TestKey = Trim(TestKey)
		Testkey = Replace(TestKey, Chr(34), "")
		Testkey = Lower(TestKey)
		If Testkey = Key Then
			Value$ = Piece(Test, 2, "=")
			Value$ = Trim(Value$)
			Value$ = Replace(Value$, Chr(34), "")
			Return(Value)
		EndIf
		If (Not p) Then Return(DefaultValue)
		Properties = Right(Properties, Len(Properties) - p)
	Forever 
End Function

Include "Source Code\Drawportals.bb"

Type Forest
	Field TileMesh%[6]
	Field DetailMesh%[6]
	Field TileTexture%[10]
	Field Grid%[(GridSize * GridSize) + 11]
	Field TileEntities%[(GridSize * GridSize) + 1]
	Field Forest_Pivot%
	Field Door%[2]
	Field DetailEntities%[2]
	Field ID%
End Type

Function Move_Forward%(Dir%, PathX%, PathY%, RetVal% = 0)
	; ~ Move 1 unit along the grid in the designated direction
	If Dir = 1 Then
		If RetVal = 0 Then
			Return(PathX)
		Else
			Return(PathY + 1)
		EndIf
	EndIf
	If RetVal = 0 Then
		Return(PathX - 1 + Dir)
	Else
		Return(PathY)
	EndIf
End Function

Function Turn_If_Deviating%(Max_Deviation_Distance_%, Pathx%, Center_%, Dir%, RetVal% = 0)
	; ~ Check if deviating and return the answer. if deviating, turn around
	Local Current_Deviation% = Center_ - Pathx
	Local Deviated% = False
	
	If (Dir = 0 And Current_Deviation >= Max_Deviation_Distance_) Or (Dir = 2 And Current_Deviation =< -Max_Deviation_Distance_) Then
		Dir = (Dir + 2) Mod 4
		Deviated = True
	EndIf
	If RetVal = 0 Then 
		Return(Dir) 
	Else 
		Return(Deviated)
	EndIf
End Function

Function GenForestGrid(fr.Forest)
	CatchErrors("Uncaught (GenForestGrid)")
	fr\ID = LastForestID + 1
	LastForestID = LastForestID + 1
	
	Local Door1_Pos%, Door2_Pos%
	Local i%, j%
	
	Door1_Pos = Rand(3, 7)
	Door2_Pos = Rand(3, 7)
	
	; ~ Clear the grid
	For i = 0 To GridSize - 1
		For j = 0 To GridSize - 1
			fr\Grid[(j * GridSize) + i] = 0
		Next
	Next
	
	; ~ Set the position of the concrete and doors
	fr\Grid[Door1_Pos] = 3
	fr\Grid[((GridSize - 1) * GridSize) + Door2_Pos] = 3
	
	; ~ Generate the path
	Local PathX% = Door2_Pos
	Local PathY% = 1
	Local Dir% = 1
	
	fr\Grid[((GridSize - 1 - PathY) * GridSize) + PathX] = 1
	
	Local Deviated%
	
	While PathY < GridSize -4
		If Dir = 1 Then ; ~ Determine whether to go forward or to the side
			If Chance(Deviation_Chance) Then
				; ~ Pick a branch direction
				Dir = 2 * Rand(0, 1)
				; ~ Make sure you have not passed max side distance
				Dir = Turn_If_Deviating(Max_Deviation_Distance, PathX, Center, Dir)
				Deviated = Turn_If_Deviating(Max_Deviation_Distance, PathX, Center, Dir, 1)
				If Deviated Then fr\Grid[((GridSize - 1 - PathY) * GridSize) + PathX] = 1
				PathX = Move_Forward(Dir, PathX, PathY)
				PathY = Move_Forward(Dir, PathX, PathY, 1)
			EndIf
		Else
			; ~ We are going to the side, so determine whether to keep going or go forward again
			Dir = Turn_If_Deviating(Max_Deviation_Distance, PathX, Center, Dir)
			Deviated = Turn_If_Deviating(Max_Deviation_Distance, PathX, Center, Dir, 1)
			If Deviated Or Chance(Return_Chance) Then Dir = 1
			
			PathX = Move_Forward(Dir, PathX, PathY)
			PathY = Move_Forward(Dir, PathX, PathY, 1)
			; ~ If we just started going forward go twice so as to avoid creating a potential 2x2 line
			If Dir = 1 Then
				fr\Grid[((GridSize - 1 - PathY) * GridSize) + PathX] = 1
				PathX = Move_Forward(Dir, PathX, PathY)
				PathY = Move_Forward(Dir, PathX, PathY, 1)
			EndIf
		EndIf
		;~ Add our position to the grid
		fr\Grid[((GridSize - 1 - PathY) * GridSize) + PathX] = 1
	Wend
	; ~ Finally, bring the path back to the door now that we have reached the end
	Dir = 1
	While PathY < GridSize - 2
		PathX = Move_Forward(Dir, PathX, PathY)
		PathY = Move_Forward(Dir, PathX, PathY, 1)
		fr\Grid[((GridSize - 1 - PathY) * GridSize) + PathX] = 1
	Wend
	
	If PathX <> Door1_Pos Then
		Dir = 0
		If Door1_Pos > PathX Then Dir = 2
		While PathX <> Door1_Pos
			PathX = Move_Forward(Dir, PathX, PathY)
			PathY = Move_Forward(Dir, PathX, PathY, 1)
			fr\Grid[((GridSize - 1 - PathY) * GridSize) + PathX] = 1
		Wend
	EndIf
	
	; ~ Attempt to create new branches
	Local New_Y%, Temp_Y%, New_X%
	Local Branch_Type%, Branch_Pos%
	
	New_Y = -3 ; ~ Used for counting off. Branches will only be considered once every 4 units so as to avoid potentially too many branches
	While New_Y < GridSize - 6
		New_Y = New_Y + 4
		Temp_Y = New_Y
		New_X = 0
		If Chance(Branch_Chance) Then
			Branch_Type = -1
			If Chance(Cobble_Chance) Then
				Branch_Type = -2
			EndIf
			; ~ Create a branch at this spot
			; ~ Determine if on left or on right
			Branch_Pos = 2 * Rand(0, 1)
			; ~ Get leftmost or rightmost path in this row
			LeftMost = GridSize
			RightMost = 0
			For i = 0 To GridSize
				If fr\Grid[((GridSize - 1 - New_Y) * GridSize) + i] = 1 Then
					If i < LeftMost Then LeftMost = i
					If i > RightMost Then RightMost = i
				EndIf
			Next
			If Branch_Pos = 0 Then New_X = LeftMost - 1 Else New_X = RightMost + 1
			; ~ Before creating a branch make sure there are no 1's above or below
			If (Temp_Y <> 0 And fr\Grid[((GridSize - 1 - Temp_Y + 1) * GridSize) + New_X] = 1) Or fr\Grid[((GridSize - 1 - Temp_Y - 1) * GridSize) + New_X] = 1 Then
				Exit ; ~ Break simply to stop creating the branch
			EndIf
			fr\Grid[((GridSize - 1 - Temp_Y) * GridSize) + New_X] = Branch_Type ; ~ Make 4s so you don't confuse your branch for a path; will be changed later
			If Branch_Pos = 0 Then New_X = LeftMost - 2 Else New_X = rightmost + 2
			fr\Grid[((GridSize - 1 - Temp_Y) * GridSize) + New_X] = Branch_Type ; ~ Branch out twice to avoid creating an unwanted 2x2 path with the real path
			i = 2
			While i < Branch_Max_Life
				i = i + 1
				If Chance(Branch_Die_Chance) Then
					Exit
				EndIf
				If Rand(0, 3) = 0 Then ; ~ Have a higher chance to go up to confuse the player
					If Branch_Pos = 0 Then
						New_X = New_X - 1
					Else
						New_X = New_X + 1
					EndIf
				Else
					Temp_Y = Temp_Y + 1
				EndIf
				
				; ~ Before creating a branch make sure there are no 1's above or below
				n = ((GridSize - 1 - Temp_Y + 1) * GridSize) + New_X
				If n < GridSize - 1 Then 
					If Temp_Y <> 0 And fr\Grid[n] = 1 Then Exit
				EndIf
				n = ((GridSize - 1 - Temp_Y - 1) * GridSize) + New_X
				If n > 0 Then 
					If fr\Grid[n] = 1 Then Exit
				EndIf
				fr\Grid[((GridSize - 1 - Temp_Y) * GridSize) + New_X] = Branch_Type ; ~ Make 4s so you don't confuse your branch for a path; will be changed later
				If Temp_Y >= GridSize - 2 Then Exit
			Wend
		EndIf
	Wend
	
	; ~ Change branches from 4s to 1s (they were 4s so that they didn't accidently create a 2x2 path unintentionally)
	For i = 0 To GridSize - 1
		For j = 0 To GridSize - 1
			If fr\Grid[(i * GridSize) + j] = -1 Then
				fr\Grid[(i * GridSize) + j] = 1
			ElseIf fr\Grid[(i * GridSize) + j] = -2
				fr\Grid[(i * GridSize) + j] = 1
			EndIf
		Next
	Next
	
	CatchErrors("GenForestGrid")
End Function

Function PlaceForest(fr.Forest, x#, y#, z#, r.Rooms)
	CatchErrors("Uncaught (PlaceForest)")
	
	Local tX%, tY%
	Local Tile_Size# = 12.0
	Local Tile_Type%
	Local Tile_Entity%, Detail_Entity%
	Local Tempf1#, Tempf2#, Tempf3#
	Local i%
	
	If fr\Forest_Pivot <> 0 Then FreeEntity(fr\Forest_Pivot) : fr\Forest_Pivot = 0
	For i = 0 To 3
		If fr\TileMesh[i] <> 0 Then FreeEntity(fr\TileMesh[i]) : fr\TileMesh[i] = 0
	Next
	For i = 0 To 4
		If fr\DetailMesh[i] <> 0 Then FreeEntity(fr\DetailMesh[i]) : fr\DetailMesh[i] = 0
	Next
	For i = 0 To 9
		If fr\TileTexture[i] <> 0 Then FreeEntity(fr\TileTexture[i]) : fr\TileTexture[i] = 0
	Next
	
	fr\Forest_Pivot = CreatePivot()
	PositionEntity(fr\Forest_Pivot, x, y, z, True)
	
	; ~ Load assets
	Local hMap%[ROOM4], Mask%[ROOM4]
	Local GroundTexture% = LoadTexture_Strict("GFX\map\forest\forestfloor.jpg")
	Local PathTexture% = LoadTexture_Strict("GFX\map\forest\forestpath.jpg")
	
	hMap[ROOM1] = LoadImage_Strict("GFX\map\forest\forest1h.png")
	Mask[ROOM1] = LoadTexture_Strict("GFX\map\forest\forest1h_mask.png", 1 + 2)
	
	hMap[ROOM2] = LoadImage_Strict("GFX\map\forest\forest2h.png")
	Mask[ROOM2] = LoadTexture_Strict("GFX\map\forest\forest2h_mask.png", 1 + 2)
	
	hMap[ROOM2C] = LoadImage_Strict("GFX\map\forest\forest2Ch.png")
	Mask[ROOM2C] = LoadTexture_Strict("GFX\map\forest\forest2Ch_mask.png", 1 + 2)
	
	hMap[ROOM3] = LoadImage_Strict("GFX\map\forest\forest3h.png")
	Mask[ROOM3] = LoadTexture_Strict("GFX\map\forest\forest3h_mask.png", 1 + 2)
	
	hMap[ROOM4] = LoadImage_Strict("GFX\map\forest\forest4h.png")
	Mask[ROOM4] = LoadTexture_Strict("GFX\map\forest\forest4h_mask.png", 1 + 2)
	
	For i = ROOM1 To ROOM4
		fr\TileMesh[i] = LoadTerrain(hMap[i], 0.03, GroundTexture, PathTexture, Mask[i])
	Next
	
	; ~ Detail meshes
	fr\DetailMesh[1] = LoadMesh_Strict("GFX\map\forest\detail\treetest4.b3d")
	fr\DetailMesh[2] = LoadMesh_Strict("GFX\map\forest\detail\rock.b3d")
	fr\DetailMesh[3] = LoadMesh_Strict("GFX\map\forest\detail\rock2.b3d")
	fr\DetailMesh[4] = LoadMesh_Strict("GFX\map\forest\detail\treetest5.b3d")
	fr\DetailMesh[5] = LoadMesh_Strict("GFX\map\forest\wall.b3d")
	
	For i = ROOM1 To ROOM4
		HideEntity(fr\TileMesh[i])
	Next
	For i = 1 To 5
		HideEntity(fr\DetailMesh[i])
	Next
	
	Tempf3 = MeshWidth(fr\TileMesh[ROOM1])
	Tempf1 = Tile_Size / Tempf3
	
	For tX = 1 To GridSize - 1
		For tY = 1 To GridSize - 1
			If fr\Grid[(tY * GridSize) + tX] = 1 Then 
				Tile_Type = 0
				If tX + 1 < GridSize Then Tile_Type = (fr\Grid[(tY * GridSize) + tX + 1] > 0)
				If tX - 1 >= 0 Then Tile_Type = Tile_Type + (fr\Grid[(tY * GridSize) + tX - 1] > 0)
				
				If tY + 1 < GridSize Then Tile_Type = Tile_Type + (fr\Grid[((tY + 1) * GridSize) + tX] > 0)
				If tY - 1 >= 0 Then Tile_Type = Tile_Type + (fr\Grid[((tY - 1) * GridSize) + tX] > 0)
				
				Local Angle% = 0
				
				Select Tile_Type
					Case 1
						;[Block]
						Tile_Entity = CopyEntity(fr\TileMesh[ROOM1])
						
						If fr\Grid[((tY + 1) * GridSize) + tX] > 0 Then
							Angle = 180
						ElseIf fr\Grid[(tY * GridSize) + tX - 1] > 0
							Angle = 270
						ElseIf fr\Grid[(tY * GridSize) + tX + 1] > 0
							Angle = 90
						End If
						
						Tile_Type = ROOM1
						;[End Block]
					Case 2
						;[Block]
						If fr\Grid[((tY - 1) * GridSize) + tX] > 0 And fr\Grid[((tY + 1) * GridSize) + tX] > 0 Then
							Tile_Entity = CopyEntity(fr\TileMesh[ROOM2])
							Tile_Type = ROOM2
						ElseIf fr\Grid[(tY * GridSize) + tX + 1] > 0 And fr\Grid[(tY * GridSize) + tX - 1] > 0
							Tile_Entity = CopyEntity(fr\TileMesh[ROOM2])
							Angle = 90
							Tile_Type = ROOM2
						Else
							Tile_Entity = CopyEntity(fr\TileMesh[ROOM2C])
							If fr\Grid[(tY * GridSize) + tX - 1] > 0 And fr\Grid[((tY + 1) * GridSize) + tX] > 0 Then
								Angle = 180
							ElseIf fr\Grid[(tY * GridSize) + tX + 1] > 0 And fr\Grid[((tY - 1) * GridSize) + tX] > 0
								
							ElseIf fr\Grid[(tY * GridSize) + tX - 1] > 0 And fr\Grid[((tY - 1) * GridSize) + tX] > 0
								Angle = 270
							Else
								Angle = 90
							EndIf
							Tile_Type = ROOM2C
						EndIf
						;[End Block]
					Case 3
						;[Block]
						Tile_Entity = CopyEntity(fr\TileMesh[ROOM3])
						
						If fr\Grid[((tY - 1) * GridSize) + tX] = 0 Then
							Angle = 180
						ElseIf fr\Grid[(tY * GridSize) + tX - 1] = 0
							Angle = 90
						ElseIf fr\Grid[(tY * GridSize) + tX + 1] = 0
							Angle = 270
						End If
						
						Tile_Type = ROOM3
						;[End Block]
					Case 4
						;[Block]
						Tile_Entity = CopyEntity(fr\TileMesh[ROOM4])	
						Tile_Type = ROOM4
						;[End Block]
				End Select
				
				If Tile_Type > 0 Then 
					Local ItemPlaced[4]
					Local it.Items = Null
					
					If (tY Mod 3) = 2 And ItemPlaced[Floor(tY / 3)] = False Then
						ItemPlaced[Floor(tY / 3)] = True
						it.Items = CreateItem("Log #" + Int(Floor(tY / 3) + 1), "paper", 0.0, 0.5, 0.0)
						EntityType(it\Collider, HIT_ITEM)
						EntityParent(it\Collider, Tile_Entity)
					EndIf
					
					; ~ Place trees and other details
					; ~ Only placed on spots where the value of the heightmap is above 100
					SetBuffer(ImageBuffer(hMap[Tile_Type]))
					Width = ImageWidth(hMap[Tile_Type])
					Tempf4# = (Tempf3 / Float(Width))
					For lx = 3 To Width - 2
						For ly = 3 To Width - 2
							GetColor(lx, Width - ly)
							If ColorRed() > Rand(100, 260) Then
								Select Rand(0, 7)
									Case 0, 1, 2, 3, 4, 5, 6 ; ~ Create a tree
										;[Block]
										Detail_Entity = CopyEntity(fr\DetailMesh[1])
										Tempf2 = Rnd(0.25, 0.4)
										
										For i = 0 To 3
											d = CopyEntity(fr\DetailMesh[4])
											RotateEntity(d, 0.0, 90.0 * i + Rnd(-20.0, 20.0), 0.0)
											EntityParent(d,Detail_Entity)
											EntityFX(d, 1)
										Next
										
										ScaleEntity(Detail_Entity, Tempf2 * 1.1, Tempf2, Tempf2 * 1.1, True)
										PositionEntity(Detail_Entity, lx * Tempf4 - (Tempf3 / 2.0), ColorRed() * 0.03 - Rnd(3.0, 3.2), ly * Tempf4 - (Tempf3 / 2.0), True)
										RotateEntity(Detail_Entity, Rnd(-5.0, 5.0), Rnd(360.0), 0.0, True)
										;[End Block]
									Case 7 ; ~ Add a rock
										;[Block]
										Detail_Entity = CopyEntity(fr\DetailMesh[2])
										Tempf2 = Rnd(0.01, 0.012)
										PositionEntity(Detail_Entity, lx * Tempf4 - (Tempf3 / 2.0), ColorRed() * 0.03 - 1.3, ly * Tempf4 - (Tempf3 / 2.0), True)
										EntityFX(Detail_Entity, 1)
										RotateEntity(Detail_Entity, 0.0, Rnd(360.0), 0.0, True)
										;[End Block]
									Case 6 ; ~ Add a stump
										;[Block]
										Detail_Entity = CopyEntity(fr\DetailMesh[4])
										Tempf2 = Rnd(0.1, 0.12)
										ScaleEntity(Detail_Entity, Tempf2, Tempf2, Tempf2, True)
										PositionEntity(Detail_Entity, lx * Tempf4 - (Tempf3 / 2.0), ColorRed() * 0.03 - 1.3, ly * Tempf4 - (Tempf3 / 2.0), True)
										;[End Block]
								End Select
								EntityFX(Detail_Entity, 1)
								EntityParent(Detail_Entity, Tile_Entity)
							EndIf
						Next
					Next
					SetBuffer(BackBuffer())
					
					TurnEntity(Tile_Entity, 0.0, Angle, 0.0)
					PositionEntity(Tile_Entity, x + (tX * Tile_Size), y, z + (tY * Tile_Size), True)
					ScaleEntity(Tile_Entity, Tempf1, Tempf1, Tempf1)
					EntityType(Tile_Entity, HIT_MAP)
					EntityFX(Tile_Entity, 1)
					EntityParent(Tile_Entity, fr\Forest_Pivot)
					EntityPickMode(Tile_Entity, 2)
					
					If it <> Null Then EntityParent(it\Collider, 0)
					
					fr\TileEntities[tX + (tY * GridSize)] = Tile_Entity
				EndIf
			EndIf
		Next
	Next
	
	; ~ Place the wall		
	For i = 0 To 1
		tY = ((GridSize - 1) * i)
		For tX = 1 To GridSize - 1
			If fr\Grid[(tY * GridSize) + tX] = 3 Then
				fr\DetailEntities[i] = CopyEntity(fr\DetailMesh[5])
				ScaleEntity(fr\DetailEntities[i], RoomScale, RoomScale, RoomScale)
				
				fr\Door[i] = CopyEntity(r\Objects[3])
				PositionEntity(fr\Door[i], 72.0 * RoomScale, 32.0 * RoomScale, 0.0, True)
				RotateEntity(fr\Door[i], 0.0, 180.0, 0.0)
				ScaleEntity(fr\Door[i], 48.0 * RoomScale, 45.0 * RoomScale, 48.0 * RoomScale, True)
				EntityParent(fr\Door[i], fr\DetailEntities[i])
				
				Frame = CopyEntity(r\Objects[2], fr\Door[i])
				PositionEntity(Frame, 0.0, 32.0 * RoomScale, 0.0, True)
				ScaleEntity(Frame, 48.0 * RoomScale, 45.0 * RoomScale, 48.0 * RoomScale, True)
				EntityParent(Frame, fr\DetailEntities[i])
				
				EntityType(fr\DetailEntities[i], HIT_MAP)
				EntityPickMode(fr\DetailEntities[i], 2)
				PositionEntity(fr\DetailEntities[i], x + (tX * Tile_Size), y, z + (tY * Tile_Size) + (Tile_Size / 2) - (Tile_Size * i), True)
				RotateEntity(fr\DetailEntities[i], 0.0, 180.0 * i, 0.0)
				EntityParent(fr\DetailEntities[i], fr\Forest_Pivot)
			EndIf		
		Next		
	Next
	
	CatchErrors("PlaceForest")
End Function

Function PlaceForest_MapCreator(fr.Forest, x#, y#, z#, r.Rooms)
	CatchErrors("Uncaught (PlaceForest_MapCreator)")
	
	Local tX%, tY%
	Local Tile_Size# = 12.0
	Local Tile_Type%
	Local Tile_Entity%, Eetail_Entity%
	Local Tempf1#, Tempf2#, Tempf3#
	Local i%
	
	If fr\Forest_Pivot <> 0 Then FreeEntity(fr\Forest_Pivot) : fr\Forest_Pivot = 0
	For i = 0 To 3
		If fr\TileMesh[i] <> 0 Then FreeEntity(fr\TileMesh[i]) : fr\TileMesh[i] = 0
	Next
	For i = 0 To 4
		If fr\DetailMesh[i] <> 0 Then FreeEntity(fr\DetailMesh[i]) : fr\DetailMesh[i] = 0
	Next
	For i = 0 To 9
		If fr\TileTexture[i] <> 0 Then FreeEntity(fr\TileTexture[i]) : fr\TileTexture[i] = 0
	Next
	
	fr\Forest_Pivot = CreatePivot()
	PositionEntity(fr\Forest_Pivot, x, y, z, True)
	
	Local hMap%[ROOM4], Mask%[ROOM4]
	; ~ Load assets
	Local GroundTexture% = LoadTexture_Strict("GFX\map\forest\forestfloor.jpg")
	Local PathTexture% = LoadTexture_Strict("GFX\map\forest\forestpath.jpg")
	
	hMap[ROOM1] = LoadImage_Strict("GFX\map\forest\forest1h.png")
	Mask[ROOM1] = LoadTexture_Strict("GFX\map\forest\forest1h_mask.png", 1 + 2)
	
	hMap[ROOM2] = LoadImage_Strict("GFX\map\forest\forest2h.png")
	Mask[ROOM2] = LoadTexture_Strict("GFX\map\forest\forest2h_mask.png", 1 + 2)
	
	hMap[ROOM2C] = LoadImage_Strict("GFX\map\forest\forest2Ch.png")
	Mask[ROOM2C] = LoadTexture_Strict("GFX\map\forest\forest2Ch_mask.png", 1 + 2)
	
	hMap[ROOM3] = LoadImage_Strict("GFX\map\forest\forest3h.png")
	Mask[ROOM3] = LoadTexture_Strict("GFX\map\forest\forest3h_mask.png", 1 + 2)
	
	hMap[ROOM4] = LoadImage_Strict("GFX\map\forest\forest4h.png")
	Mask[ROOM4] = LoadTexture_Strict("GFX\map\forest\forest4h_mask.png", 1 + 2)
	
	For i = ROOM1 To ROOM4
		fr\TileMesh[i] = LoadTerrain(hMap[i], 0.03, GroundTexture, PathTexture, Mask[i])
	Next
	
	; ~ Detail meshes
	fr\DetailMesh[1] = LoadMesh_Strict("GFX\map\forest\detail\treetest4.b3d")
	fr\DetailMesh[2] = LoadMesh_Strict("GFX\map\forest\detail\rock.b3d")
	fr\DetailMesh[3] = LoadMesh_Strict("GFX\map\forest\detail\rock2.b3d")
	fr\DetailMesh[4] = LoadMesh_Strict("GFX\map\forest\detail\treetest5.b3d")
	fr\DetailMesh[5] = LoadMesh_Strict("GFX\map\forest\wall.b3d")
	
	For i = ROOM1 To ROOM4
		HideEntity(fr\TileMesh[i])
	Next
	For i = 1 To 5
		HideEntity(fr\DetailMesh[i])
	Next
	
	Tempf3 = MeshWidth(fr\TileMesh[ROOM1])
	Tempf1 = Tile_Size / Tempf3
	
	For tX = 0 To GridSize - 1
		For tY = 0 To GridSize - 1
			If fr\Grid[(tY * GridSize) + tX] > 0 Then 
				
				Tile_Type = 0
				
				Local Angle% = 0
				
				Tile_Type = Ceil(Float(fr\Grid[(tY * GridSize) + tX]) / 4.0)
				If Tile_Type = 6 Then
					Tile_Type = 2
				EndIf
				Angle = (fr\Grid[(tY * GridSize) + tX] Mod 4) * 90
				
				Tile_Entity = CopyEntity(fr\TileMesh[Tile_Type])
				
				If Tile_Type > 0 Then 
					Local ItemPlaced[4]
					Local it.Items = Null
					
					If (tY Mod 3) = 2 And ItemPlaced[Floor(tY / 3)] = False Then
						ItemPlaced[Floor(tY / 3)] = True
						it.Items = CreateItem("Log #" + Int(Floor(tY / 3) + 1), "paper", 0.0, 0.5, 0.0)
						EntityType(it\Collider, HIT_ITEM)
						EntityParent(it\Collider, Tile_Entity)
					EndIf
					
					; ~ Place trees and other details
					; ~ Only placed on spots where the value of the heightmap is above 100
					SetBuffer(ImageBuffer(hMap[Tile_Type]))
					Width = ImageWidth(hMap[Tile_Type])
					Tempf4# = (Tempf3 / Float(Width))
					For lx = 3 To Width - 2
						For ly = 3 To width - 2
							GetColor lx, width - ly
							If ColorRed() > Rand(100, 260) Then
								Detail_Entity = 0
								Select Rand(0, 7)
									Case 0, 1, 2, 3, 4, 5, 6 ; ~ Create a tree
										;[Block]
										Detail_Entity = CopyEntity(fr\DetailMesh[1])
										Tempf2 = Rnd(0.25, 0.4)
										
										For i = 0 To 3
											d = CopyEntity(fr\DetailMesh[4])
											RotateEntity(d, 0.0, 90.0 * i + Rnd(-20.0, 20.0), 0.0)
											EntityParent(d, Detail_entity)
											EntityFX(d, 1)
										Next
										ScaleEntity(Detail_Entity, Tempf2 * 1.1, Tempf2, Tempf2 * 1.1, True)
										PositionEntity(Detail_Entity, lx * Tempf4 - (Tempf3 / 2.0), ColorRed() * 0.03 - Rnd(3.0, 3.2), ly * Tempf4 - (Tempf3 / 2.0), True)
										
										RotateEntity(Detail_Entity, Rnd(-5.0, 5.0), Rnd(360.0), 0.0, True)
										;[End Block]
									Case 7 ; ~ Add a rock
										;[Block]
										Detail_Entity = CopyEntity(fr\DetailMesh[2])
										Tempf2 = Rnd(0.01, 0.012)
										PositionEntity(Detail_Entity, lx * Tempf4 - (Tempf3 / 2.0), ColorRed() * 0.03 - 1.3, ly * Tempf4 - (Tempf3 / 2.0), True)
										EntityFX(Detail_Entity, 1)
										RotateEntity(Detail_Entity, 0.0, Rnd(360.0), 0.0, True)
										;[End Block]
									Case 6 ; ~ Add a stump
										;[Block]
										Detail_Entity = CopyEntity(fr\DetailMesh[4])
										Tempf2 = Rnd(0.1, 0.12)
										ScaleEntity(Detail_Entity, Tempf2, Tempf2, Tempf2, True)
										PositionEntity(Detail_Entity, lx * Tempf4 - (Tempf3 / 2.0), ColorRed() * 0.03 - 1.3, ly * Tempf4 - (Tempf3 / 2.0), True)
										;[End Block]
								End Select
								
								If Detail_Entity <> 0 Then
									EntityFX(Detail_Entity, 1)
									EntityParent(Detail_Entity, Tile_Entity)
								EndIf
							EndIf
						Next
					Next
					SetBuffer(BackBuffer())
					
					TurnEntity(Tile_Entity, 0.0, Angle, 0.0)
					PositionEntity(Tile_Entity, x + (tX * Tile_Size), y, z + (tY * Tile_Size), True)
					ScaleEntity(Tile_Entity, Tempf1, Tempf1, Tempf1)
					EntityType(Tile_Entity, HIT_MAP)
					EntityFX(Tile_Entity, 1)
					EntityParent(Tile_Entity, fr\Forest_Pivot)
					EntityPickMode(Tile_Entity, 2)
					
					If it <> Null Then EntityParent(it\Collider, 0)
					
					fr\TileEntities[tX + (tY * GridSize)] = Tile_Entity
				EndIf
				
				If Ceil(fr\Grid[(tY * GridSize) + tX] / 4.0) = 6 Then
					For i = 0 To 1
						If fr\Door[i] = 0 Then
							fr\DetailEntities[i] = CopyEntity(fr\DetailMesh[5])
							ScaleEntity(fr\DetailEntities[i], RoomScale, RoomScale, RoomScale)
							
							fr\Door[i] = CopyEntity(r\Objects[3])
							PositionEntity(fr\Door[i], 72.0 * RoomScale, 32.0 * RoomScale, 0.0, True)
							RotateEntity(fr\Door[i], 0.0, 180.0, 0.0)
							ScaleEntity(fr\Door[i], 48.0 * RoomScale, 45.0 * RoomScale, 48.0 * RoomScale, True)
							EntityParent(fr\Door[i], fr\DetailEntities[i])
							
							Local Frame% = CopyEntity(r\Objects[2], fr\Door[i])
							
							PositionEntity(Frame, 0.0, 32.0 * RoomScale, 0.0, True)
							ScaleEntity(Frame, 48.0 * RoomScale, 45.0 * RoomScale, 48.0 * RoomScale, True)
							EntityParent(Frame, fr\DetailEntities[i])
							
							EntityType(fr\DetailEntities[i], HIT_MAP)
							EntityPickMode(fr\DetailEntities[i], 2)
							PositionEntity(fr\DetailEntities[i], x + (tX * Tile_Size), y, z + (tY * Tile_Size), True)
							RotateEntity(fr\DetailEntities[i], 0.0, Angle + 180.0, 0.0)
							MoveEntity(fr\DetailEntities[i], 0.0, 0.0, -6.0)
							EntityParent(fr\DetailEntities[i], fr\Forest_Pivot)
							Exit
						EndIf
					Next
				EndIf
			EndIf
		Next
	Next
	
	CatchErrors("PlaceForest_MapCreator")
End Function

Function DestroyForest(fr.Forest)
	CatchErrors("Uncaught (DestroyForest)")
	Local tX%, tY%, i%
	
	For tX% = 0 To GridSize - 1
		For tY% = 0 To GridSize - 1
			If fr\TileEntities[tX + (tY * GridSize)] <> 0 Then
				FreeEntity(fr\TileEntities[tX + (tY * GridSize)])
				fr\TileEntities[tX + (tY * GridSize)] = 0
				fr\Grid[tX + (tY * GridSize)] = 0
			EndIf
		Next
	Next
	If fr\Door[0] <> 0 Then FreeEntity(fr\Door[0]) : fr\Door[0] = 0
	If fr\Door[1] <> 0 Then FreeEntity(fr\Door[1]) : fr\Door[0] = 1
	If fr\DetailEntities[0] <> 0 Then FreeEntity(fr\DetailEntities[0]) : fr\DetailEntities[0] = 0
	If fr\DetailEntities[1] <> 0 Then FreeEntity(fr\DetailEntities[1]) : fr\DetailEntities[1] = 0
	
	If fr\Forest_Pivot <> 0 Then FreeEntity(fr\Forest_Pivot) : fr\Forest_Pivot = 0
	For i = 0 To 3
		If fr\TileMesh[i] <> 0 Then FreeEntity(fr\TileMesh[i]) : fr\TileMesh[i] = 0
	Next
	For i = 0 To 4
		If fr\DetailMesh[i] <> 0 Then FreeEntity(fr\DetailMesh[i]) : fr\DetailMesh[i] = 0
	Next
	For i = 0 To 9
		If fr\TileTexture[i] <> 0 Then FreeEntity(fr\TileTexture[i]) : fr\TileTexture[i] = 0
	Next
	
	CatchErrors("DestroyForest")
End Function

Function UpdateForest(fr.Forest, Ent%)
	CatchErrors("Uncaught (UpdateForest)")
	
	Local tX%, tY%
	
	If Abs(EntityY(Ent, True) - EntityY(fr\Forest_Pivot, True)) < 12.0 Then
		For tX = 0 To GridSize - 1
			For tY = 0 To GridSize - 1
				If fr\TileEntities[tX + (tY * GridSize)] <> 0 Then
					If Abs(EntityX(Ent, True) - EntityX(fr\TileEntities[tX + (tY * GridSize)], True)) < 20.0 Then
						If Abs(EntityZ(Ent, True) - EntityZ(fr\TileEntities[tX + (tY * GridSize)], True)) < 20.0 Then
							ShowEntity(fr\TileEntities[tX + (tY * GridSize)])
						Else
							HideEntity(fr\TileEntities[tX + (tY * GridSize)])
						EndIf
					Else
						HideEntity(fr\TileEntities[tX + (tY * GridSize)])
					EndIf
				EndIf
			Next
		Next
	EndIf
	
	CatchErrors("UpdateForest")
End Function

Global RoomTempID%

Type RoomTemplates
	Field OBJ%, ID%
	Field OBJPath$
	Field Zone%[5]
	Field TempSoundEmitter%[MaxRoomEmitters]
	Field TempSoundEmitterX#[MaxRoomEmitters], TempSoundEmitterY#[MaxRoomEmitters], TempSoundEmitterZ#[MaxRoomEmitters]
	Field TempSoundEmitterRange#[MaxRoomEmitters]
	Field Shape%, Name$
	Field Commonness%, Large%
	Field DisableDecals%
	Field TempTriggerboxAmount
	Field TempTriggerbox[128]
	Field TempTriggerboxName$[128]
	Field UseLightSpark%
	Field DisableOverlapCheck% = True
	Field MinX#, MinY#, MinZ#
	Field MaxX#, MaxY#, MaxZ#
End Type 	

Function CreateRoomTemplate.RoomTemplates(MeshPath$)
	Local rt.RoomTemplates = New RoomTemplates
	
	rt\OBJPath = MeshPath
	
	rt\ID = RoomTempID
	RoomTempID = RoomTempID + 1
	
	Return(rt)
End Function

Function LoadRoomTemplates(File$)
	CatchErrors("Uncaught (LoadRoomTemplates)")
	
	Local TemporaryString$, i%
	Local rt.RoomTemplates = Null
	Local StrTemp$ = ""
	
	Local f% = OpenFile(File)
	
	While Not Eof(f)
		TemporaryString = Trim(ReadLine(f))
		If Left(TemporaryString, 1) = "[" Then
			TemporaryString = Mid(TemporaryString, 2, Len(TemporaryString) - 2)
			StrTemp = GetINIString(File, TemporaryString, "mesh path")
			
			rt = CreateRoomTemplate(StrTemp)
			rt\Name = Lower(TemporaryString)
			
			StrTemp = Lower(GetINIString(File, TemporaryString, "shape"))
			
			Select StrTemp
				Case "room1", "1"
					;[Block]
					rt\Shape = ROOM1
					;[End Block]
				Case "room2", "2"
					;[Block]
					rt\Shape = ROOM2
					;[End Block]
				Case "room2c", "2c"
					;[Block]
					rt\Shape = ROOM2C
					;[End Block]
				Case "room3", "3"
					;[Block]
					rt\Shape = ROOM3
					;[End Block]
				Case "room4", "4"
					;[Block]
					rt\Shape = ROOM4
					;[End Block]
			End Select
			
			For i = 0 To 4
				rt\Zone[i] = GetINIInt(File, TemporaryString, "zone" + (i + 1))
			Next
			
			rt\Commonness = Max(Min(GetINIInt(File, TemporaryString, "commonness"), 100), 0)
			rt\Large = GetINIInt(File, TemporaryString, "large")
			rt\DisableDecals = GetINIInt(File, TemporaryString, "disabledecals")
			rt\UseLightSpark = GetINIInt(File, TemporaryString, "uselightspark")
			rt\DisableOverlapCheck = GetINIInt(File, TemporaryString, "disableoverlapcheck")
		EndIf
	Wend
	
	i = 1
	Repeat
		StrTemp = GetINIString(File, "room ambience", "ambience" + i)
		If StrTemp = "" Then Exit
		
		RoomAmbience[i] = LoadSound_Strict(StrTemp)
		i = i + 1
	Forever
	
	CloseFile(f)
	
	CatchErrors("LoadRoomTemplates")
End Function

Function LoadRoomMesh(rt.RoomTemplates)
	If Instr(rt\OBJPath, ".rmesh") <> 0 Then ; ~ File is .rmesh
		rt\OBJ = LoadRMesh(rt\OBJPath, rt)
	Else ; ~ File is .b3d
		If rt\OBJPath <> "" Then rt\OBJ = LoadWorld(rt\OBJPath, rt) Else rt\OBJ = CreatePivot()
	EndIf
	
	If (Not rt\OBJ) Then RuntimeError("Failed to load map file " + Chr(34) + MapFile + Chr(34) + ".")
	
	CalculateRoomTemplateExtents(rt)
	
	HideEntity(rt\OBJ)
End Function

Function LoadRoomMeshes()
	Local Temp% = 0
	
	For rt.RoomTemplates = Each RoomTemplates
		Temp = Temp + 1
	Next	
	
	Local i% = 0
	
	For rt.RoomTemplates = Each RoomTemplates
		If Instr(rt\OBJPath, ".rmesh") <> 0 Then ; ~ File is .rmesh
			rt\obj = LoadRMesh(rt\OBJPath, rt)
		Else ; ~ File is .b3d
			If rt\OBJPath <> "" Then rt\obj = LoadWorld(rt\OBJPath, rt) Else rt\OBJ = CreatePivot()
		EndIf
		If (Not rt\OBJ) Then RuntimeError "Failed to load map file " + Chr(34) + MapFile + Chr(34) + "."
		
		HideEntity(rt\OBJ)
		DrawLoading(Int(30.0 + (15.0 / Temp) * i))
		i = i + 1
	Next
End Function

LoadRoomTemplates("Data\rooms.ini")

Global RoomScale# = 8.0 / 2048.0

Dim MapTemp%(MapWidth + 1, MapHeight + 1)
Dim MapFound%(MapWidth + 1, MapHeight + 1)

Global RoomAmbience%[20]

Global Sky%

Global HideDistance# = 15.0

Global SecondaryLightOn# = True
Global PrevSecondaryLightOn# = True
Global RemoteDoorOn% = True

Type Rooms
	Field Zone%
	Field Found%
	Field OBJ%
	Field x#, y#, z#
	Field Angle%
	Field RoomTemplate.RoomTemplates
	Field Dist#
	Field SoundCHN%
	Field dp.DrawPortal, fr.Forest
	Field SoundEmitter%[MaxRoomEmitters]
	Field SoundEmitterOBJ%[MaxRoomEmitters]
	Field SoundEmitterRange#[MaxRoomEmitters]
	Field SoundEmitterCHN%[MaxRoomEmitters]
	Field Lights%[MaxRoomLights]
	Field LightIntensity#[MaxRoomLights]
	Field LightSprites%[MaxRoomLights]	
	Field Objects%[MaxRoomObjects]
	Field Levers%[11]
	Field RoomDoors.Doors[7]
	Field NPC.NPCs[12]
	Field grid.Grids
	Field Adjacent.Rooms[4]
	Field AdjDoor.Doors[4]
	Field NonFreeAble%[10]
	Field Textures%[10]
	Field MaxLights% = 0
	Field LightSpriteHidden%[MaxRoomLights]
	Field LightSpritesPivot%[MaxRoomLights]
	Field LightSprites2%[MaxRoomLights]
	Field LightHidden%[MaxRoomLights]
	Field LightFlicker%[MaxRoomLights]
	Field AlarmRotor%[1]
	Field AlarmRotorLight%[1]
	Field TriggerboxAmount
	Field Triggerbox[128]
	Field TriggerboxName$[128]
	Field MaxWayPointY#
	Field LightR#[MaxRoomLights], LightG#[MaxRoomLights], LightB#[MaxRoomLights]
	Field LightSpark%[MaxRoomLights]
	Field LightSparkTimer#[MaxRoomLights]
	Field MinX#, MinY#, MinZ#
	Field MaxX#, MaxY#, MaxZ#
End Type 

Type Grids
	Field Grid%[GridSZ * GridSZ]
	Field Angles%[GridSZ * GridSZ]
	Field Meshes%[7]
	Field Entities%[GridSZ * GridSZ]
	Field waypoints.WayPoints[GridSZ * GridSZ]
End Type

Function UpdateGrid(grid.Grids)
	Local tX%, tY%
	
	For tX = 0 To GridSZ - 1
		For tY = 0 To GridSZ - 1
			If grid\Entities[tX + (tY * GridSZ)] <> 0 Then
				If Abs(EntityY(Collider, True) - EntityY(grid\Entities[tX + (tY * GridSZ)], True)) > 4.0 Then Exit
				If Abs(EntityX(Collider, True) - EntityX(grid\Entities[tX + (tY * GridSZ)], True)) < HideDistance Then
					If Abs(EntityZ(Collider, True) - EntityZ(grid\Entities[tX + (tY * GridSZ)], True)) < HideDistance Then
						ShowEntity(grid\Entities[tX + (tY * GridSZ)])
					Else
						HideEntity(grid\Entities[tX + (tY * GridSZ)])
					EndIf
				Else
					HideEntity(grid\Entities[tX + (tY * GridSZ)])
				EndIf
			EndIf
		Next
	Next
End Function

Function PlaceGrid_MapCreator(r.Rooms)
	Local x%, y%, i%
	Local Meshes[6]
	Local dr.Doors, it.Items
	Local o.Objects = First Objects
	
	For i = 0 To 6
		Meshes[i] = CopyEntity(o\MTModelID[i])
		HideEntity(Meshes[i])
	Next
	
	For y = 0 To (GridSZ - 1)
		For x = 0 To (GridSZ - 1)
			If r\grid\Grid[x + (y * GridSZ)] > 0 Then
				Local Tile_Type% = 0
				Local Angle% = 0
				
				Tile_Type = r\grid\Grid[x + (y * GridSZ)]
				Angle = r\grid\Angles[x +(y * GridSZ)] * 90
				
				Local Tile_Entity% = CopyEntity(Meshes[Tile_Type - 1])
				
				RotateEntity(Tile_Entity, 0.0, Angle, 0.0)
				ScaleEntity(Tile_Entity, RoomScale, RoomScale, RoomScale, True)
				PositionEntity(Tile_Entity, r\x + x * 2.0, 8.0, r\z + y * 2.0, True)
				
				Select r\grid\Grid[x + (y * GridSZ)]
					Case ROOM1
						;[Block]
						AddLight%(Null, r\x + x * 2.0, 8.0 + (368.0 * RoomScale), r\z + y * 2.0, 2, 500.0 * RoomScale, 255, 255, 255)
						;[End Block]
					Case ROOM2, ROOM2C
						;[Block]
						AddLight%(Null, r\x + x * 2.0, 8.0 + (368.0 * RoomScale), r\z + y * 2.0, 2, 500.0 * RoomScale, 255, 255, 255)
						;[End Block]
					Case ROOM2C
						;[Block]
						AddLight%(Null, r\x + x * 2.0, 8.0 + (412.0 * RoomScale), r\z + y * 2.0, 2, 500.0 * RoomScale, 255, 255, 255)
						;[End Block]
					Case ROOM3, ROOM4
						;[Block]
						AddLight%(Null, r\x + x * 2.0, 8.0 + (412.0 * RoomScale), r\z + y * 2.0, 2, 500.0 * RoomScale, 255, 255, 255)
						;[End Block]
					Case ROOM4 + 1
						;[Block]
						dr = CreateDoor(r\Zone, r\x + (x * 2.0) + (Cos(EntityYaw(Tile_Entity, True)) * 240.0 * RoomScale), 8.0, r\z + (y * 2.0) + (Sin(EntityYaw(Tile_Entity, True)) * 240.0 * RoomScale), EntityYaw(Tile_Entity, True) + 90.0, Null, False, 3)
						PositionEntity(dr\Buttons[0], EntityX(dr\Buttons[0], True) + (Cos(EntityYaw(Tile_Entity, True)) * 0.05), EntityY(dr\buttons[0], True) + 0.0, EntityZ(dr\buttons[0], True) + (Sin(EntityYaw(Tile_Entity, True)) * 0.05), True)
						
						AddLight%(Null, r\x + x * 2.0 + (Cos(EntityYaw(Tile_Entity, True)) * 555.0 * RoomScale), 8.0 + (469.0 * RoomScale), r\z + y * 2.0 + (Sin(EntityYaw(Tile_Entity, True)) * 555.0 * RoomScale), 2, 600.0 * RoomScale, 255, 255, 255)
						
						Local TempInt2% = CreatePivot()
						
						RotateEntity(TempInt2, 0.0, EntityYaw(Tile_Entity, True) + 180.0, 0.0, True)
						PositionEntity(TempInt2, r\x + (x * 2.0) + (Cos(EntityYaw(Tile_Entity, True)) * 552.0 * RoomScale), 8.0 + (240.0 * RoomScale), r\z + (y * 2.0) + (Sin(EntityYaw(Tile_Entity, True)) * 552.0 * RoomScale))
						If r\RoomDoors[1] = Null Then
							r\RoomDoors[1] = dr
							r\Objects[3] = TempInt2
							PositionEntity(r\Objects[0], r\x + x * 2.0, 8.0, r\z + y * 2.0, True)
						ElseIf r\RoomDoors[1] <> Null And r\RoomDoors[3] = Null Then
							r\RoomDoors[3] = dr
							r\Objects[5] = TempInt2
							PositionEntity(r\Objects[1], r\x + x * 2.0, 8.0, r\z + y * 2.0, True)
						EndIf
						;[End Block]
					Case ROOM4 + 2
						;[Block]
						AddLight%(Null, r\x + x * 2.0 - (Sin(EntityYaw(Tile_Entity, True)) * 504.0 * RoomScale) + (Cos(EntityYaw(Tile_Entity, True)) * 16.0 * RoomScale), 8.0 + (396.0 * RoomScale), r\z + y * 2.0 + (Cos(EntityYaw(Tile_Entity, True)) * 504.0 * RoomScale) + (Sin(EntityYaw(Tile_Entity, True)) * 16.0 * RoomScale), 2, 500.0 * RoomScale, 255, 200, 200)
						it = CreateItem("SCP-500-01", "scp500", r\x + x * 2.0 + (Cos(EntityYaw(Tile_Entity, True)) * (-208.0) * RoomScale) - (Sin(EntityYaw(Tile_Entity, True)) * 1226.0 * RoomScale), 8.0 + (80.0 * RoomScale), r\z + y * 2.0 + (Sin(EntityYaw(Tile_Entity, True)) * (-208.0) * RoomScale) + (Cos(EntityYaw(Tile_Entity, True)) * 1226.0 * RoomScale))
						EntityType(it\Collider, HIT_ITEM)
						
						it = CreateItem("Night Vision Goggles", "nvgoggles", r\x + x * 2.0 - (Sin(EntityYaw(Tile_Entity, True)) * 504.0 * RoomScale) + (Cos(EntityYaw(Tile_Entity, True)) * 16.0 * RoomScale), 8.0 + (80.0 * RoomScale),  r\z + y * 2.0 + (Cos(EntityYaw(Tile_Entity, True)) * 504.0 * RoomScale) + (Sin(EntityYaw(Tile_Entity, True)) * 16.0 * RoomScale))
						EntityType(it\Collider, HIT_ITEM)
						;[End Block]
				End Select
				
				r\grid\Entities[x + (y * GridSZ)] = Tile_Entity
				wayp.WayPoints = CreateWaypoint(r\x + (x * 2.0), 8.2, r\z + (y * 2.0), Null, r)
				r\grid\waypoints[x + (y * GridSZ)] = Wayp
				
				If y < GridSZ - 1 Then
					If r\grid\waypoints[x + ((y + 1) * GridSZ)] <> Null Then
						Dist = EntityDistance(r\grid\waypoints[x + (y * GridSZ)]\OBJ, r\grid\waypoints[x + ((y + 1) * GridSZ)]\OBJ)
						For i = 0 To 3
							If r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + ((y + 1) * GridSZ)] Then
								Exit 
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + ((y + 1) * GridSZ)]
								r\grid\waypoints[x + (y * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
						For i = 0 To 3
							If r\grid\waypoints[x + ((y + 1) * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + ((y + 1) * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + ((y + 1) * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)]
								r\grid\waypoints[x + ((y + 1) * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
					EndIf
				EndIf
				If y > 0 Then
					If r\grid\waypoints[x + ((y - 1) * GridSZ)] <> Null Then
						Dist = EntityDistance(r\grid\waypoints[x + (y * GridSZ)]\OBJ, r\grid\waypoints[x + ((y - 1) * GridSZ)]\OBJ)
						For i = 0 To 3
							If r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + ((y - 1) * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + ((y - 1) * GridSZ)]
								r\grid\waypoints[x + (y * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
						For i = 0 To 3
							If r\grid\waypoints[x + ((y - 1) * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + ((y - 1) * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)]
								r\grid\waypoints[x + ((y - 1) * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
					EndIf
				EndIf
				If x > 0 Then
					If r\grid\waypoints[x - 1 + (y * GridSZ)] <> Null Then
						Dist = EntityDistance(r\grid\waypoints[x + (y * GridSZ)]\OBJ, r\grid\waypoints[x - 1 + (y * GridSZ)]\OBJ)
						For i = 0 To 3
							If r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x - 1 + (y * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x - 1 + (y * GridSZ)]
								r\grid\waypoints[x + (y * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
						For i = 0 To 3
							If r\grid\waypoints[x - 1 + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x - 1 + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)]
								r\grid\waypoints[x - 1 + (y * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
					EndIf
				EndIf
				If x < GridSZ - 1 Then
					If r\grid\waypoints[x + 1 + (y * GridSZ)] <> Null Then
						Dist = EntityDistance(r\grid\waypoints[x + (y * GridSZ)]\OBJ, r\grid\waypoints[x + 1 + (y * GridSZ)]\OBJ)
						For i = 0 To 3
							If r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + 1 + (y * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + 1 + (y * GridSZ)]
								r\grid\waypoints[x + (y * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
						For i = 0 To 3
							If r\grid\waypoints[x + 1 + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)] Then
								Exit
							ElseIf r\grid\waypoints[x + (y * GridSZ)]\connected[i] = Null Then
								r\grid\waypoints[x + 1 + (y * GridSZ)]\connected[i] = r\grid\waypoints[x + (y * GridSZ)]
								r\grid\waypoints[x + 1 + (y * GridSZ)]\Dist[i] = Dist
								Exit
							EndIf
						Next
					EndIf
				EndIf
			EndIf
		Next
	Next
	
	For i = 0 To 6
		r\grid\Meshes[i] = Meshes[i]
	Next
End Function

Function CreateRoom.Rooms(Zone%, RoomShape%, x#, y#, z#, Name$ = "")
	CatchErrors("Uncaught (CreateRoom)")
	Local r.Rooms = New Rooms
	Local rt.RoomTemplates
	
	r\Zone = Zone
	
	r\x = x : r\y = y : r\z = z
	
	If Name <> "" Then
		Name = Lower(Name)
		For rt.RoomTemplates = Each RoomTemplates
			If rt\Name = Name Then
				r\RoomTemplate = rt
				
				If rt\OBJ = 0 Then LoadRoomMesh(rt)
				
				r\OBJ = CopyEntity(rt\OBJ)
				ScaleEntity(r\OBJ, RoomScale, RoomScale, RoomScale)
				EntityType(r\OBJ, HIT_MAP)
				EntityPickMode(r\OBJ, 2)
				
				PositionEntity(r\OBJ, x, y, z)
				FillRoom(r)
				
				If r\RoomTemplate\UseLightSpark
					UpdateLightSpark(r)
				EndIf
				
				CalculateRoomExtents(r)
				Return r
			EndIf
		Next
	EndIf
	
	Local Temp% = 0
	
	For rt.RoomTemplates = Each RoomTemplates
		Local i%
		
		For i = 0 To 4
			If rt\Zone[i] = Zone Then 
				If rt\Shape = RoomShape Then Temp = Temp + rt\Commonness : Exit
			EndIf
		Next
	Next
	
	Local RandomRoom% = Rand(Temp)
	
	Temp = 0
	For rt.RoomTemplates = Each RoomTemplates
		For i = 0 To 4
			If rt\Zone[i] = Zone And rt\Shape = RoomShape Then
				Temp = Temp + rt\Commonness
				If RandomRoom > Temp - rt\Commonness And RandomRoom =< Temp Then
					r\RoomTemplate = rt
					
					If rt\OBJ = 0 Then LoadRoomMesh(rt)
					
					r\OBJ = CopyEntity(rt\OBJ)
					ScaleEntity(r\OBJ, RoomScale, RoomScale, RoomScale)
					EntityType(r\OBJ, HIT_MAP)
					EntityPickMode(r\OBJ, 2)
					
					PositionEntity(r\OBJ, x, y, z)
					FillRoom(r)
					
					If r\RoomTemplate\UseLightSpark
						UpdateLightSpark(r)
					EndIf
					
					CalculateRoomExtents(r)
					Return r	
				End If
			EndIf
		Next
	Next
	
	CatchErrors("CreateRoom")
End Function

Function FillRoom(r.Rooms)
	CatchErrors("Uncaught (FillRoom)")
	Local d.Doors, d2.Doors, sc.SecurityCams, de.Decals, r2.Rooms, sc2.SecurityCams
	Local it.Items
	Local xTemp%, yTemp%, zTemp%
	Local t1%
	Local scX#, scY#, scZ#, scAngle#
	Local iX#, iy#, iZ#
	Local dX#, dZ#, dSize#, dID%
	Local i%, k%
	Local o.Objects = First Objects
	
	Select r\RoomTemplate\Name
		Case "room860"
			;[Block]
			; ~ Doors to observation booth
			d = CreateDoor(r\Zone, r\x + 928.0 * RoomScale, r\y, r\z + 640.0 * RoomScale, 0.0, r, False, False, False, "GEAR")
			d\AutoClose = False
			
			d = CreateDoor(r\Zone, r\x + 928.0 * RoomScale, r\y, r\z - 640.0 * RoomScale, 0.0, r, True, False, False, "GEAR")
			d\AutoClose = False
			
			; ~ Doors to the room itself
			d = CreateDoor(r\Zone, r\x + 416.0 * RoomScale, r\y, r\z - 640.0 * RoomScale, 0.0, r, False, False, 3)
			d\AutoClose = False
			
			d = CreateDoor(r\Zone, r\x + 416.0 * RoomScale, r\y, r\z + 640.0 * RoomScale, 0.0, r, False, False, 3)
			d\AutoClose = False
			
			; ~ The wooden doors
			r\Objects[2] = CopyEntity(o\DoorModelID[8])
			PositionEntity(r\Objects[2], r\x + 184.0 * RoomScale, r\y, r\z)
			ScaleEntity(r\Objects[2], 45.0 * RoomScale, 45.0 * RoomScale, 80.0 * RoomScale)
			
			r\Objects[3] = CopyEntity(o\DoorModelID[9])
			PositionEntity(r\Objects[3], r\x + 112.0 * RoomScale, r\y, r\z + 0.05)
			EntityType(r\Objects[3], HIT_MAP)
			ScaleEntity(r\Objects[3], 46.0 * RoomScale, 45.0 * RoomScale, 46.0 * RoomScale)
			
			r\Objects[4] = CopyEntity(r\Objects[3])
			PositionEntity(r\Objects[4], r\x + 256.0 * RoomScale, r\y, r\z - 0.05)
			RotateEntity(r\Objects[4], 0.0, 180.0, 0.0)
			ScaleEntity(r\Objects[4], 46.0 * RoomScale, 45.0 * RoomScale, 46.0 * RoomScale)
			
			For i = 2 To 4
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			; ~ The forest
			If I_Zone\HasCustomForest = False Then
				Local fr.Forest = New Forest
				
				r\fr = fr
				GenForestGrid(fr)
				PlaceForest(fr, r\x, r\y + 30.0, r\z, r)
			EndIf
			
			it = CreateItem("Document SCP-860-1", "paper", r\x + 672.0 * RoomScale, r\y + 176.0 * RoomScale, r\z + 335.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle + 10, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-860", "paper", r\x + 1152.0 * RoomScale, r\y + 176.0 * RoomScale, r\z - 384.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle + 170, 0.0)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2clockroom"
			;[Block]
			; ~ Doors
			d = CreateDoor(r\Zone, r\x - 736.0 * RoomScale, r\y, r\z - 104.0 * RoomScale, 0.0, r, True)
			d\Timer = 70 * 5 : d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], r\x - 288.0 * RoomScale, EntityY(d\Buttons[0], True), r\z - 634.0 * RoomScale, True)
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			d2 = CreateDoor(r\Zone, r\x + 104.0 * RoomScale, r\y, r\z + 736.0 * RoomScale, 270.0, r, True)
			d2\Timer = 70 * 5 : d2\AutoClose = False: d2\Open = False
			PositionEntity(d2\Buttons[0], r\x + 634.0 * RoomScale, r\y + 0.7, r\z + 288.0 * RoomScale, True)
			RotateEntity(d2\Buttons[0], 0.0, 90.0, 0.0, True)
			FreeEntity(d2\Buttons[1]) : d2\Buttons[1] = 0
			
			d\LinkedDoor = d2
			d2\LinkedDoor = d
			
			; ~ Security camera inside
			sc = CreateSecurityCam(r\x - 688.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 688.0 * RoomScale, r, True)
			sc\Angle = 45 + 180
			sc\Turn = 45
			sc\ScrTexture = 1
			EntityTexture(sc\ScrOBJ, ScreenTexs[sc\ScrTexture])
			TurnEntity(sc\CameraOBJ, 40.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			PositionEntity(sc\ScrOBJ, r\x + 668.0 * RoomScale, r\y + 1.1, r\z - 96.0 * RoomScale)
			TurnEntity(sc\ScrOBJ, 0.0, 90.0, 0.0)
			EntityParent(sc\ScrOBJ, r\OBJ)
			
			sc = CreateSecurityCam(r\x - 112.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 112.0 * RoomScale, r, True)
			sc\Angle = 45 : sc\Turn = 45 : sc\ScrTexture = 1
			EntityTexture(sc\ScrOBJ, ScreenTexs[sc\ScrTexture])
			TurnEntity(sc\CameraOBJ, 40.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)				
			PositionEntity(sc\ScrOBJ, r\x + 96.0 * RoomScale, r\y + 1.1, r\z - 668.0 * RoomScale)
			EntityParent(sc\ScrOBJ, r\OBJ)
			
			; ~ Smoke
			Local em.Emitters = CreateEmitter(r\x - 175.0 * RoomScale, r\y + 370.0 * RoomScale, r\z + 656.0 * RoomScale, 0.0)
			
			TurnEntity(em\OBJ, 90.0, 0.0, 0.0)
			EntityParent(em\OBJ, r\OBJ)
			em\RandAngle = 20.0
			em\Speed = 0.05
			em\SizeChange = 0.007
			em\Achange = -0.006
			em\Gravity = -0.24
			
			em = CreateEmitter(r\x - 655.0 * RoomScale, 370.0 * RoomScale, r\z + 240.0 * RoomScale, 0)
			TurnEntity(em\OBJ, 90.0, 0.0, 0.0)
			EntityParent(em\OBJ, r\OBJ)
			em\RandAngle = 20.0
			em\Speed = 0.05
			em\SizeChange = 0.007
			em\Achange = -0.006
			em\Gravity = -0.24
			;[End Block]
		Case "room2clockroom3"
			;[Block]
			; ~ Security cameras inside
			sc = CreateSecurityCam(r\x + 512.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 384.0 * RoomScale, r, True)
			sc\Angle = 45.0 + 90.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 40.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			PositionEntity(sc\ScrOBJ, r\x + 668.0 * RoomScale, r\y + 1.1, r\z - 96.0 * RoomScale)
			TurnEntity(sc\ScrOBJ, 0.0, 90.0, 0.0)
			EntityParent(sc\ScrOBJ, r\OBJ)
			
			sc = CreateSecurityCam(r\x - 384.0 * RoomScale, r\y + 384.0 * RoomScale, r\z - 512.0 * RoomScale, r, True)
			sc\Angle = 45.0 + 90.0 + 180.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 40.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)				
			PositionEntity(sc\ScrOBJ, r\x + 96.0 * RoomScale, r\y + 1.1, r\z - 668.0 * RoomScale)
			EntityParent(sc\ScrOBJ, r\OBJ)
			
			; ~ Create blood decals inside
			For i = 0 To 5
				de = CreateDecal(Rand(2, 3), r\x + Rnd(-392.0, 520.0) * RoomScale, r\y + 3.0 * RoomScale + Rnd(0, 0.001), r\z + Rnd(-392.0, 520.0) * RoomScale, 90.0, Rnd(360.0), 0.0)
				de\Size = Rnd(0.3, 0.6)
				ScaleSprite(de\OBJ, de\Size, de\Size)
				de = CreateDecal(Rand(15, 16), r\x + Rnd(-392.0, 520.0) * RoomScale, r\y + 3.0 * RoomScale + Rnd(0, 0.001), r\z + Rnd(-392.0, 520.0) * RoomScale, 90.0, Rnd(360.0), 0.0)
				de\Size = Rnd(0.1, 0.6)
				ScaleSprite(de\OBJ, de\Size, de\Size)
				de = CreateDecal(Rand(15, 16), r\x + Rnd(-0.5, 0.5), r\y + 3.0 * RoomScale + Rnd(0, 0.001), r\z + Rnd(-0.5, 0.5), 90.0, Rnd(360.0), 0.0)
				de\Size = Rnd(0.1, 0.6)
				ScaleSprite(de\OBJ, de\Size, de\Size)
			Next
			;[End Block]
		Case "gatea"
			;[Block]
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x - 4064.0 * RoomScale, r\y - 1280.0 * RoomScale, r\z + 3952.0 * RoomScale, 0.0, r, False)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = False
			
			d2 = CreateDoor(r\Zone, r\x, r\y, r\z - 1024.0 * RoomScale, 0.0, r)
			d2\AutoClose = False : d2\Open = False : d2\Locked = True
			FreeEntity(d2\Buttons[0]) : d2\Buttons[0] = 0
			
			d2 = CreateDoor(r\Zone, r\x - 1440.0 * RoomScale, r\y - 480.0 * RoomScale, r\z + 2328.0 * RoomScale, 0.0, r, False, False, 2)
			If SelectedEnding = "A2" Then 
				d2\AutoClose = False : d2\Open = True : d2\Locked = True	
			Else
				d2\AutoClose = False : d2\Open = False : d2\Locked = False	
			EndIf	
			PositionEntity(d2\Buttons[0], r\x - 1320.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z + 2294.0 * RoomScale, True)
			PositionEntity(d2\Buttons[1], r\x - 1590.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z + 2484.0 * RoomScale, True)	
			RotateEntity(d2\Buttons[1], 0.0, 90.0, 0.0, True)
			
			d2 = CreateDoor(r\Zone, r\x - 1440 * RoomScale, r\y - 480.0 * RoomScale, r\z + 4352.0 * RoomScale, 0.0, r, False, False, 2)
			If SelectedEnding = "A2" Then 
				d2\AutoClose = False : d2\Open = True : d2\Locked = True	
			Else
				d2\AutoClose = False : d2\Open = False : d2\Locked = False
			EndIf
			PositionEntity(d2\Buttons[0], r\x - 1320.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z + 4378.0 * RoomScale, True)
			RotateEntity(d2\Buttons[0], 0.0, 180.0, 0.0, True)
			PositionEntity(d2\Buttons[1], r\x - 1590.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z + 4232.0 * RoomScale, True)	
			RotateEntity(d2\Buttons[1], 0.0, 90.0, 0.0, True)
			
			For r2.Rooms = Each Rooms
				If r2\RoomTemplate\Name = "exit1" Then
					r\Objects[1] = r2\Objects[1]
					r\Objects[2] = r2\Objects[2]	
				ElseIf r2\RoomTemplate\Name = "gateaentrance"
					; ~ Elevator
					r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 1544.0 * RoomScale, r\y, r\z - 64.0 * RoomScale, 90.0, r, False, 3)
					r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False
					PositionEntity(r\RoomDoors[1]\Buttons[0], r\x + 1578.0 * RoomScale, EntityY(r\RoomDoors[1]\Buttons[0], True), r\z + 80.0 * RoomScale, True)
					PositionEntity(r\RoomDoors[1]\Buttons[1], r\x + 1462.0 * RoomScale, EntityY(r\RoomDoors[1]\Buttons[1], True), r\z - 208.0 * RoomScale, True)
					
					r2\Objects[1] = CreatePivot()
					PositionEntity(r2\Objects[1], r\x + 1848.0 * RoomScale, r\y + 240.0 * RoomScale, r\z - 64.0 * RoomScale)
					EntityParent(r2\Objects[1], r\OBJ)						
				EndIf
			Next
			
			; ~ SCP-106's spawnpoint
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x + 1216.0 * RoomScale, r\y, r\z + 2112.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x, r\y + 96.0 * RoomScale, r\z + 6400.0 * RoomScale)		
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x + 1784.0 * RoomScale, r\y + 2124.0 * RoomScale, r\z + 4512.0 * RoomScale)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x - 5048.0 * RoomScale, r\y + 1912.0 * RoomScale, r\z + 4656.0 * RoomScale)	
			
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x + 1824.0 * RoomScale, r\y + 224.0 * RoomScale, r\z + 7056.0 * RoomScale)	
			
			r\Objects[8] = CreatePivot()
			PositionEntity(r\Objects[8], r\x - 1824.0 * RoomScale, r\y + 224.0 * RoomScale, r\z + 7056.0 * RoomScale)	
			
			r\Objects[9] = CreatePivot()
			PositionEntity(r\Objects[9], r\x + 2624.0 * RoomScale, r\y + 992.0 * RoomScale, r\z + 6157.0 * RoomScale)	
			
			For i = 3 To 9
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[11] = CreatePivot()
			PositionEntity(r\Objects[11], r\x - 4064.0 * RoomScale, r\y - 1248.0 * RoomScale, r\z - 1696.0 * RoomScale)
			EntityParent(r\Objects[11], r\OBJ)
			
			r\Objects[13] = LoadMesh_Strict("GFX\map\gateawall1.b3d", r\OBJ)
			PositionEntity(r\Objects[13], r\x - 4308.0 * RoomScale, r\y - 1045.0 * RoomScale, r\z + 544.0 * RoomScale, True)
			EntityColor(r\Objects[13], 25.0, 25.0, 25.0)
			EntityType(r\Objects[13], HIT_MAP)
			
			r\Objects[14] = LoadMesh_Strict("GFX\map\gateawall2.b3d", r\OBJ)
			PositionEntity(r\Objects[14], r\x - 3820.0 * RoomScale, r\y - 1045.0 * RoomScale, r\z + 544.0 * RoomScale, True)	
			EntityColor(r\Objects[14], 25.0, 25.0, 25.5)
			EntityType(r\Objects[14], HIT_MAP)
			
			r\Objects[15] = CreatePivot()
			PositionEntity(r\Objects[15], r\x - 3568.0 * RoomScale, r\y - 1089.0 * RoomScale, r\z + 4944.0 * RoomScale)
			EntityParent(r\Objects[15], r\OBJ)
			
			; ~ Hit Box
			r\Objects[16] = LoadMesh_Strict("GFX\map\gatea_hitbox1.b3d", r\OBJ)
			EntityPickMode(r\Objects[16], 2)
			EntityType(r\Objects[16], HIT_MAP)
			EntityAlpha(r\Objects[16], 0.0)
			;[End Block]
		Case "gateaentrance"
			;[Block]
			; ~ Elevator
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 736.0 * RoomScale, r\y, r\z + 512.0 * RoomScale, 90.0, r, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True : r\RoomDoors[0]\Locked = 2
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True) - 0.061, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True)
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True) + 0.061, EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True), True)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x, r\y, r\z - 360.0 * RoomScale, 0.0, r, False, True, 5)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False
			PositionEntity(r\RoomDoors[1]\Buttons[1], r\x + 422.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[1], True), r\z - 576.0 * RoomScale, True)
			RotateEntity(r\RoomDoors[1]\Buttons[1], 0.0, r\Angle - 90, 0.0, True)
			PositionEntity(r\RoomDoors[1]\Buttons[0], r\x - 522.0 * RoomScale, EntityY(r\RoomDoors[1]\Buttons[0], True), EntityZ(r\RoomDoors[1]\Buttons[0], True), True)
			RotateEntity(r\RoomDoors[1]\Buttons[0], 0.0, r\Angle - 225, 0.0, True)
			
			; ~ Elevator's pivot
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 1048.0 * RoomScale, r\y, r\z + 512.0 * RoomScale)
			EntityParent(r\Objects[0], r\OBJ)
			;[End Block]
		Case "exit1"
			;[Block]
			; ~ Elevators
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 720.0 * RoomScale, r\y, r\z + 1432.0 * RoomScale, 180.0, r, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True), EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True) - 0.031, True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True), EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True) + 0.031, True)	
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x - 5424.0 * RoomScale, r\y + 10784.0 * RoomScale, r\z - 1380.0 * RoomScale, 180.0, r, False, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False
			PositionEntity(r\RoomDoors[1]\Buttons[0], EntityX(r\RoomDoors[1]\Buttons[0], True), EntityY(r\RoomDoors[1]\Buttons[0], True), EntityZ(r\RoomDoors[1]\Buttons[0], True) - 0.031, True)
			PositionEntity(r\RoomDoors[1]\Buttons[1], EntityX(r\RoomDoors[1]\Buttons[1], True), EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\Buttons[1], True) + 0.031, True)
			
			; ~ Other doors
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x + 4352.0 * RoomScale, r\y + 10784.0 * RoomScale, r\z - 492.0 * RoomScale, 0.0, r)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = False	
			
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x + 4352.0 * RoomScale, r\y + 10784.0 * RoomScale, r\z + 500.0 * RoomScale, 0.0, r)
			r\RoomDoors[3]\AutoClose = False : r\RoomDoors[3]\Open = False	
			
			r\RoomDoors[4] = CreateDoor(r\Zone, r\x, r\y, r\z - 320.0 * RoomScale, 0.0, r, False, True, 5)
			r\RoomDoors[4]\AutoClose = False : r\RoomDoors[4]\Open = False : r\RoomDoors[4]\Locked = 2
			PositionEntity(r\RoomDoors[4]\Buttons[1], r\x + 358.0 * RoomScale, EntityY(r\RoomDoors[4]\Buttons[1], True), r\z - 528.0 * RoomScale, True)
			RotateEntity(r\RoomDoors[4]\Buttons[1], 0.0, r\Angle - 90, 0.0, True)
			PositionEntity(r\RoomDoors[4]\Buttons[0], EntityX(r\RoomDoors[4]\Buttons[0], True), EntityY(r\RoomDoors[4]\Buttons[0], True), r\z - 198.0 * RoomScale, True)
			RotateEntity(r\RoomDoors[4]\Buttons[0], 0.0, r\Angle - 180, 0.0, True)
			
			r\RoomDoors[5] = CreateDoor(r\Zone, r\x + 3248.0 * RoomScale, r\y + 9856.0 * RoomScale, r\z + 6400.0 * RoomScale, 0.0, r, False, False, False, "GEAR")
			r\RoomDoors[5]\AutoClose = False : r\RoomDoors[5]\Open = False		
			FreeEntity(r\RoomDoors[5]\Buttons[1]) : r\RoomDoors[5]\Buttons[1] = 0	
			
			d = CreateDoor(r\Zone, r\x + 3072.0 * RoomScale, r\y + 9856.0 * RoomScale, r\z + 5800.0 * RoomScale, 90.0, r, False, False, 3)
			d\AutoClose = False : d\Open = False
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 4356.0 * RoomScale, r\y + 9767.0 * RoomScale, r\z + 2588.0 * RoomScale)
			EntityParent(r\Objects[0], r\OBJ)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 7680.0 * RoomScale, r\y + 10992.0 * RoomScale, r\z - 27048.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x + 5203.36 * RoomScale, r\y + 12128.0 * RoomScale, r\z - 1739.19 * RoomScale)
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x + 4363.02 * RoomScale, r\y + 10536.0 * RoomScale, r\z + 2766.16 * RoomScale)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 5192.0 * RoomScale, r\y + 12192.0 * RoomScale, r\z - 1760.0 * RoomScale)
			
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x + 5192.0 * RoomScale, r\y + 12192.0 * RoomScale, r\z - 4352.0 * RoomScale)
			
			; ~ Elevators' pivots
			r\Objects[8] = CreatePivot()
			PositionEntity(r\Objects[8], r\x + 720.0 * RoomScale, r\y, r\z + 1744.0 * RoomScale)
			
			r\Objects[9] = CreatePivot()
			PositionEntity(r\Objects[9], r\x - 5424.0 * RoomScale, r\y + 10784.0 * RoomScale, r\z - 1068.0 * RoomScale)		
			
			; ~ Walkway
			r\Objects[10] = CreatePivot()
			PositionEntity(r\Objects[10], r\x + 4352.0 * RoomScale, r\y + 10778.0 * RoomScale, r\z + 1344.0 * RoomScale)	
			
			; ~ SCP-682
			r\Objects[11] = CreatePivot()
			PositionEntity(r\Objects[11], r\x + 2816.0 * RoomScale, r\y + 11024.0 * RoomScale, r\z - 2816.0 * RoomScale)
			
			For i = 3 To 11
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[14] = CreatePivot()
			PositionEntity(r\Objects[14], r\x + 3536.0 * RoomScale, r\y + 10256.0 * RoomScale, r\z + 5512.0 * RoomScale)
			
			r\Objects[15] = CreatePivot()
			PositionEntity(r\Objects[15], r\x + 3536.0 * RoomScale, r\y + 10256.0 * RoomScale, r\z + 5824.0 * RoomScale)
			
			r\Objects[16] = CreatePivot()
			PositionEntity(r\Objects[16], r\x + 3856.0 * RoomScale, r\y + 10256.0 * RoomScale, r\z + 5512.0 * RoomScale)
			
			r\Objects[17] = CreatePivot()
			PositionEntity(r\Objects[17], r\x + 3856.0 * RoomScale, r\y + 10256.0 * RoomScale, r\z + 5824.0 * RoomScale)
			
			; ~ MTF's spawnpoint
			r\Objects[18] = CreatePivot()
			PositionEntity(r\Objects[18], r\x + 3250.0 * RoomScale, r\y + 9896.0 * RoomScale, r\z + 6623.0 * RoomScale)
			
			r\Objects[19] = CreatePivot()
			PositionEntity(r\Objects[19], r\x + 3808.0 * RoomScale, r\y + 12320.0 * RoomScale, r\z - 13568.0 * RoomScale)
			
			For i = 14 To 19
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room372"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x, r\y, r\z - 368.0 * RoomScale, 0.0, r, True, True, 2)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x - 496.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[0], True), r\z - 278.0 * RoomScale, True) 
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True) + 0.025, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True) 
			TurnEntity(r\RoomDoors[0]\Buttons[0], 0.0, 90.0, 0.0)
			
			; ~ Hit Box
			r\Objects[3] = LoadMesh_Strict("GFX\map\room372_hb.b3d", r\OBJ)
			EntityPickMode(r\Objects[3], 2)
			EntityType(r\Objects[3], HIT_MAP)
			EntityAlpha(r\Objects[3], 0.0)
			
			it = CreateItem("Document SCP-372", "paper", r\x + 800.0 * RoomScale, r\y + 176.0 * RoomScale, r\z + 1108.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Radio Transceiver", "radio", r\x + 800.0 * RoomScale, r\y + 112.0 * RoomScale, r\z + 944.0 * RoomScale)
			it\State = 80.0
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room079"
			;[Block]
			; ~ Doors to the containment chamber
			d = CreateDoor(r\Zone, r\x, r\y - 448.0 * RoomScale, r\z + 1136.0 * RoomScale, 0.0, r, False, True, 4)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[1], r\x + 224.0 * RoomScale, r\y - 250.0 * RoomScale, r\z + 918.0 * RoomScale, True)
			PositionEntity(d\Buttons[0], r\x - 240.0 * RoomScale, r\y - 250.0 * RoomScale, r\z + 1366.0 * RoomScale, True)	
			
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 1456.0 * RoomScale, r\y - 448.0 * RoomScale, r\z + 976.0 * RoomScale, 0.0, r, False, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False : r\RoomDoors[0]\Locked = 2
			PositionEntity(r\RoomDoors[0]\Buttons[1], r\x + 1760.0 * RoomScale, r\y - 250.0 * RoomScale, r\z + 1236.0 * RoomScale, True)
			TurnEntity(r\RoomDoors[0]\Buttons[0], 0.0, -90.0 - 90.0, 0.0, True)
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x + 1760.0 * RoomScale, r\y - 240.0 * RoomScale, r\z + 740.0 * RoomScale, True)
			TurnEntity(r\RoomDoors[0]\Buttons[1], 0.0, 90.0 - 90.0, 0.0, True)
			
			; ~ DNA door
			d = CreateDoor(r\Zone, r\x + 1144.0 * RoomScale, r\y - 448.0 * RoomScale, r\z + 704.0 * RoomScale, 90.0, r, False, False, -1)
			
			r\Objects[0] = LoadAnimMesh_Strict("GFX\map\079.b3d")
			ScaleEntity(r\Objects[0], 1.3, 1.3, 1.3, True)
			PositionEntity(r\Objects[0], r\x + 1856.0 * RoomScale, r\y - 560.0 * RoomScale, r\z - 672.0 * RoomScale)
			TurnEntity(r\Objects[0], 0.0, 180.0, 0.0)
			EntityParent(r\Objects[0], r\OBJ)
			
			r\Objects[1] = CreateSprite(r\Objects[0])
			SpriteViewMode(r\Objects[1], 2)
			PositionEntity(r\Objects[1], 0.082, 0.119, 0.010)
			ScaleSprite(r\Objects[1], 0.18 * 0.5, 0.145 * 0.5)
			TurnEntity(r\Objects[1], 0.0, 13.0, 0.0)
			MoveEntity(r\Objects[1], 0.0, 0.0, -0.022)
			EntityTexture(r\Objects[1], OldAiPics(0))
			HideEntity(r\Objects[1])
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x + 1184.0 * RoomScale, r\y - 448.0 * RoomScale, r\z + 1792.0 * RoomScale)
			EntityParent(r\Objects[2], r\OBJ)
			
			de = CreateDecal(3, r\x + 1184.0 * RoomScale, r\y - 448.0 * RoomScale + 0.01, r\z + 1792.0 * RoomScale, 90.0, Rnd(360.0), 0.0)
			de\Size = 0.5
			ScaleSprite(de\OBJ, de\Size, de\Size)
			EntityParent(de\OBJ, r\OBJ)
			;[End Block]
		Case "checkpoint1"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 48.0 * RoomScale, r\y, r\z - 128.0 * RoomScale, 0.0, r, False, False, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Timer = 70 * 5
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x - 152.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[0], True), r\z - 346.0 * RoomScale, True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], r\x - 152.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[1], True), r\z + 90.0 * RoomScale, True)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x - 352.0 * RoomScale, r\y, r\z - 128.0 * RoomScale, 0.0, r, False, False, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Timer = 70 * 5
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[1]\Buttons[i]) : r\RoomDoors[1]\Buttons[i] = 0
			Next
			
			r\RoomDoors[0]\LinkedDoor = r\RoomDoors[1]
			r\RoomDoors[1]\LinkedDoor = r\RoomDoors[0]
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 720.0 * RoomScale, r\y + 120.0 * RoomScale, r\z + 333.0 * RoomScale)
			EntityParent(r\Objects[0], r\OBJ)
			
			sc = CreateSecurityCam(r\x + 192.0 * RoomScale, r\y + 704.0 * RoomScale, r\z - 960.0 * RoomScale, r)
			sc\Angle = 45.0 : sc\Turn = 0.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			
			; ~ Monitors at the both sides
			r\Objects[2] = CopyEntity(o\MonitorModelID[1], r\OBJ)
			PositionEntity(r\Objects[2], r\x - 152.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 124.0 * RoomScale, True)
			ScaleEntity(r\Objects[2], 2.0, 2.0, 2.0)
			RotateEntity(r\Objects[2], 0.0, 180.0, 0.0)
			EntityFX(r\Objects[2], 1)
			
			r\Objects[3] = CopyEntity(o\MonitorModelID[1], r\OBJ)
			PositionEntity(r\Objects[3], r\x - 152.0 * RoomScale, 384.0 * RoomScale, r\z - 380.0 * RoomScale, True)
			ScaleEntity(r\Objects[3], 2.0, 2.0, 2.0)
			RotateEntity(r\Objects[3], 0.0, 0.0, 0.0)
			EntityFX(r\Objects[3], 1)
			
			If MapTemp(Floor(r\x / 8.0), Floor(r\z / 8.0) - 1) = 0 Then
				d = CreateDoor(r\Zone, r\x, r\y, r\z  - 4.0, 0.0, r, False, False, False, "GEAR")
				FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
			EndIf
			;[End Block]
		Case "checkpoint2"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 48.0 * RoomScale, r\y, r\z + 128.0 * RoomScale, 0.0, r, False, False, 5)
			r\RoomDoors[0]\AutoClose = False
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x + 152.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[0], True), r\z - 90.0 * RoomScale, True)			
			PositionEntity(r\RoomDoors[0]\Buttons[1], r\x + 152.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[1], True), r\z + 346.0 * RoomScale, True)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 352.0 * RoomScale, r\y, r\z + 128.0 * RoomScale, 0.0, r, False, False, 5)
			r\RoomDoors[1]\AutoClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[1]\Buttons[i]) : r\RoomDoors[1]\Buttons[i] = 0
			Next
			
			r\RoomDoors[0]\LinkedDoor = r\RoomDoors[1]
			r\RoomDoors[1]\LinkedDoor = r\RoomDoors[0]
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 720.0 * RoomScale, r\y + 120.0 * RoomScale, r\z + 464.0 * RoomScale)
			EntityParent(r\Objects[0], r\OBJ)
			
			; ~ Monitors at the both sides
			r\Objects[2] = CopyEntity(o\MonitorModelID[2], r\OBJ)
			PositionEntity(r\Objects[2], r\x + 152.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 380.0 * RoomScale, True)
			ScaleEntity(r\Objects[2], 2.0, 2.0, 2.0)
			RotateEntity(r\Objects[2], 0.0, 180.0, 0.0)
			EntityFX(r\Objects[2], 1)
			
			r\Objects[3] = CopyEntity(o\MonitorModelID[2], r\OBJ)
			PositionEntity(r\Objects[3], r\x + 152.0 * RoomScale, r\y + 384.0 * RoomScale, r\z - 124.0 * RoomScale, True)
			ScaleEntity(r\Objects[3], 2.0, 2.0, 2.0)
			RotateEntity(r\Objects[3], 0.0, 0.0, 0.0)
			EntityFX(r\Objects[3], 1)
			
			r\RoomDoors[0]\Timer = 70 * 5
			r\RoomDoors[1]\Timer = 70 * 5
			
			If MapTemp(Floor(r\x / 8.0), Floor(r\z / 8.0) - 1) = 0 Then
				d = CreateDoor(r\Zone, r\x, r\y, r\z  - 4.0, 0.0, r, False, False, False, "GEAR")
				FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
			EndIf
			;[End Block]
		Case "room2pit"
			;[Block]
			; ~ Smoke
			i = 0
			For xTemp = -1 To 1 Step 2
				For zTemp = -1 To 1
					em = CreateEmitter(r\x + 202.0 * RoomScale * xTemp, 8.0 * RoomScale, r\z + 256.0 * RoomScale * zTemp, 0)
					em\RandAngle = 30 : em\Speed = 0.0045 : em\SizeChange = 0.007 : em\Achange = -0.016
					r\Objects[i] = em\OBJ
					If i < 3 Then 
						TurnEntity(em\OBJ, 0.0, -90.0, 0.0) 
					Else 
						TurnEntity(em\OBJ, 0.0, 90.0, 0.0)
					EndIf
					TurnEntity(em\OBJ, -45.0, 0.0, 0.0)
					EntityParent(em\OBJ, r\OBJ)
					i = i + 1
				Next
			Next
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 640.0 * RoomScale, r\y + 8.0 * RoomScale, r\z - 896.0 * RoomScale)
			
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x - 864.0 * RoomScale, r\y - 400.0 * RoomScale, r\z - 632.0 * RoomScale)
			
			For i = 6 To 7
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room2testroom2"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 240.0 * RoomScale, r\y, r\z + 640.0 * RoomScale, 90.0, r, False, False, 1)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False
			
			d = CreateDoor(r\Zone, r\x - 512.0 * RoomScale, r\y, r\z + 384.0 * RoomScale, 0.0, r, False, False)
			d\AutoClose = False : d\Open = False	
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) + 0.031, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.031, True)					
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 640.0 * RoomScale, r\y + 0.5, r\z - 912.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 669.0 * RoomScale, r\y + 0.5, r\z - 16.0 * RoomScale)
			
			; ~ Glass panel
			Local GlassTex% = LoadTexture_Strict("GFX\map\glass.png", 1 + 2)
			
			r\Objects[2] = CreateSprite()
			EntityTexture(r\Objects[2], GlassTex)
			SpriteViewMode(r\Objects[2], 2)
			ScaleSprite(r\Objects[2], 182.0 * RoomScale * 0.5, 192.0 * RoomScale * 0.5)
			PositionEntity(r\Objects[2], r\x - 632.0 * RoomScale, r\y + 224.0 * RoomScale, r\z - 208.0 * RoomScale)
			TurnEntity(r\Objects[2], 0.0, 180.0, 0.0)			
			HideEntity(r\Objects[2])
			FreeTexture(GlassTex)
			
			For i = 0 To 2
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Level 2 Key Card", "key2", r\x - 342.0 * RoomScale, r\y + 264.0 * RoomScale, r\z + 102.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, 180.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("S-NAV 300 Navigator", "nav", r\x - 914.0 * RoomScale, r\y + 137.0 * RoomScale, r\z + 61.0 * RoomScale)
			it\State = 20.0
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room3tunnel"
			;[Block]
			; ~ Guard's position
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 190.0 * RoomScale, r\y + 4.0 * RoomScale, r\z + 190.0 * RoomScale)
			EntityParent(r\Objects[0], r\OBJ)
			;[End Block]
		Case "room2toilets"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 1040.0 * RoomScale, r\y + 192.0 * RoomScale, r\z)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 1530.0 * RoomScale, r\y + 0.5, r\z + 512.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x + 1535.0 * RoomScale, r\y + 150.0 * RoomScale, r\z + 512.0 * RoomScale)
			
			For i = 0 To 2
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room2storage"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 1288.0 * RoomScale, r\y, r\z, 270.0, r)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x - 760.0 * RoomScale, r\y, r\z, 270.0, r)
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x - 264.0 * RoomScale, r\y, r\z, 270.0, r)
			
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x + 264.0 * RoomScale, r\y, r\z, 270.0, r)
			
			r\RoomDoors[4] = CreateDoor(r\Zone, r\x + 760.0 * RoomScale, r\y, r\z, 270.0, r)
			
			r\RoomDoors[5] = CreateDoor(r\Zone, r\x + 1288.0 * RoomScale, r\y, r\z, 270.0, r)
			
			For i = 0 To 5
				r\RoomDoors[i]\AutoClose = False : r\RoomDoors[i]\Open = False	
				MoveEntity(r\RoomDoors[i]\Buttons[0], 0.0, 0.0, -8.0)
				MoveEntity(r\RoomDoors[i]\Buttons[1], 0.0, 0.0, -8.0)
			Next
			
			it = CreateItem("Document SCP-939", "paper", r\x + 352.0 * RoomScale, r\y + 176.0 * RoomScale, r\z + 256.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle + 4, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("9V Battery", "bat", r\x + 352.0 * RoomScale, r\y + 112.0 * RoomScale, r\z + 448.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Empty Cup", "emptycup", r\x - 672.0 * RoomScale, 240.0 * RoomScale, r\z + 288.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Level 1 Key Card", "key1", r\x - 672.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 224.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2sroom"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 1440.0 * RoomScale, 224.0 * RoomScale, r\z + 32.0 * RoomScale, 90.0, r, False, False, 4)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) + 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) - 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			it = CreateItem("Some SCP-420-J", "scp420j", r\x + 1776.0 * RoomScale, r\y + 400.0 * RoomScale, r\z + 427.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Some SCP-420-J", "scp420j", r\x + 1808.0 * RoomScale, r\y + 400.0 * RoomScale, r\z + 435.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Level 5 Key Card", "key5", r\x + 2232.0 * RoomScale, r\y + 392.0 * RoomScale, r\z + 387.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Nuclear Device Document", "paper", r\x + 2248.0 * RoomScale, r\y + 440.0 * RoomScale, r\z + 372.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Radio Transceiver", "radio", r\x + 2240.0 * RoomScale, r\y + 320.0 * RoomScale, r\z + 128.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2shaft"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 1552.0 * RoomScale, r\y, r\z + 552.0 * RoomScale, 0.0, r)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), r\z + 518.0 * RoomScale, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), r\z + 575.0 * RoomScale, True)
			
			d = CreateDoor(r\Zone, r\x + 256.0 * RoomScale, r\y, r\z + 744.0 * RoomScale, 90.0, r, False, False, 2)
			d\AutoClose = False : d\Open = False
			
			; ~ Player's position after leaving the pocket dimension
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 1560.0 * RoomScale, r\y, r\z + 250.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
            PositionEntity(r\Objects[1], r\x + 1344.0 * RoomScale, r\y - 752.0 * RoomScale, r\z - 384.0 * RoomScale)
            
			r\Objects[2] = CreateButton(r\x + 1180.0 * RoomScale, r\y + 180.0 * RoomScale, r\z - 552.0 * RoomScale, 0.0, 270.0, 0.0)
			
			For i = 0 To 2
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			de = CreateDecal(3, r\x + 1334.0 * RoomScale, r\y - 796.0 * RoomScale + 0.01, r\z - 220.0 * RoomScale, 90.0, Rnd(360.0), 0.0)
            de\Size = 0.25
            ScaleSprite(de\OBJ, de\Size, de\Size)
            EntityParent(de\OBJ, r\OBJ)
			
			it = CreateItem("Level 3 Key Card", "key3", r\x + 1119.0 * RoomScale, r\y + 233.0 * RoomScale, r\z + 494.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("First Aid Kit", "firstaid", r\x + 1035.0 * RoomScale, r\y + 145.0 * RoomScale, r\z + 56.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, 90.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("9V Battery", "bat", r\x + 1930.0 * RoomScale, r\y + 97.0 * RoomScale, r\z + 256.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("9V Battery", "bat", r\x + 1061.0 * RoomScale, r\y + 161.0 * RoomScale, r\z + 494.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("ReVision Eyedrops", "eyedrops", r\x + 1930.0 * RoomScale, r\y + 225.0 * RoomScale, r\z + 128.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2poffices"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 240.0 * RoomScale, r\y, r\z + 448.0 * RoomScale, 270.0, r, False, False, False, Str(AccessCode))
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) + 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) - 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)	
			
			d = CreateDoor(r\Zone, r\x - 496.0 * RoomScale, r\y, r\z, 270.0, r, False, False, False, "GEAR")
			d\AutoClose = False : d\Open = False : d\Locked = True	
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) + 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) - 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)						
			
			d = CreateDoor(r\Zone, r\x + 240.0 * RoomScale, r\y, r\z - 576.0 * RoomScale, 270.0, r, False, False, False, "7816")
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) + 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) - 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)			
			
			it = CreateItem("Mysterious Note", "paper", r\x + 736.0 * RoomScale, r\y + 224.0 * RoomScale, r\z + 544.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)	
			
			it = CreateItem("Ballistic Vest", "vest", r\x + 608.0 * RoomScale, r\y + 112.0 * RoomScale, r\z + 32.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, 90.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Incident Report SCP-106-0204", "paper", r\x + 704.0 * RoomScale, r\y + 183.0 * RoomScale, r\z - 576.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Journal Page", "paper", r\x + 912 * RoomScale, r\y + 176.0 * RoomScale, r\z - 160.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("First Aid Kit", "firstaid", r\x + 912.0 * RoomScale, r\y + 112.0 * RoomScale, r\z - 336.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, 90.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2poffices2"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 240.0 * RoomScale, r\y, r\z + 48.0 * RoomScale, 270.0, r, False, False, 3)			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) + 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) - 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)			
			
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 432.0 * RoomScale, r\y, r\z, 90.0, r, False, False, 0.0, "1234")
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False : r\RoomDoors[0]\Locked = True	
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True) - 0.061, EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\buttons[0], True), True)
			FreeEntity(r\RoomDoors[0]\Buttons[1]) : r\RoomDoors[0]\Buttons[1] = 0
			
			de = CreateDecal(0, r\x - 808.0 * RoomScale, r\y + 0.005, r\z - 72.0 * RoomScale, 90.0, Rand(360.0), 0.0)
			EntityParent(de\OBJ, r\OBJ)
			
			de = CreateDecal(2, r\x - 808.0 * RoomScale, r\y + 0.01, r\z - 72.0 * RoomScale, 90.0, Rand(360.0), 0.0)
			de\Size = 0.3
			ScaleSprite(de\OBJ, de\Size, de\Size)
			EntityParent(de\OBJ, r\OBJ)
			
			de = CreateDecal(0, r\x - 432.0 * RoomScale, r\y + 0.01, r\z, 90.0, Rand(360.0), 0.0)
			EntityParent(de\OBJ, r\OBJ)
			
			r\Objects[0] = CreatePivot(r\OBJ)
			PositionEntity(r\Objects[0], r\x - 808.0 * RoomScale, r\y + 1.0, r\z - 72.0 * RoomScale, True)
			
			it = CreateItem("Dr. L's Burnt Note", "paper", r\x - 688.0 * RoomScale, r\y + 1.0, r\z - 16.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Dr L's Burnt Note", "paper", r\x - 808.0 * RoomScale, r\y + 1.0, r\z - 72.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("The Modular Site Project", "paper", r\x + 622.0 * RoomScale, r\y + 125.0 * RoomScale, r\z - 73.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2elevator"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 448.0 * RoomScale, r\y, r\z, 90.0, r, False, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True : r\RoomDoors[0]\Locked = True : r\RoomDoors[0]\MTFClose = False
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 888.0 * RoomScale, r\y + 240.0 * RoomScale, r\z, True)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 1024.0 * RoomScale - 0.01, r\y + 120.0 * RoomScale, r\z, True)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room2cafeteria"
			;[Block]
			; ~ SCP-294
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 1847.0 * RoomScale, r\y - 240.0 * RoomScale, r\z - 321.0 * RoomScale)
			
			; ~ Spawnpoint for the cups
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 1780.0 * RoomScale, r\y - 248.0 * RoomScale, r\z - 276.0 * RoomScale)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("cup", "cup", r\x - 508.0 * RoomScale, r\y - 187.0 * RoomScale, r\z + 284.0 * RoomScale, 240, 175, 70)
			EntityParent(it\Collider, r\OBJ) : it\Name = "Cup of Orange Juice"
			
			it = CreateItem("cup", "cup", r\x + 1412.0 * RoomScale, r\y - 187.0 * RoomScale, r\z - 716.0 * RoomScale, 87, 62, 45)
			EntityParent(it\Collider, r\OBJ) : it\Name = "Cup of Coffee"
			
			it = CreateItem("Empty Cup", "emptycup", r\x - 540.0 * RoomScale, r\y - 187.0 * RoomScale, r\z + 124.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Quarter", "25ct", r\x - 447.0 * RoomScale, r\y - 334.0 * RoomScale, r\z + 36.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			it = CreateItem("Quarter", "25ct", r\x + 1409.0 * RoomScale, r\y - 334.0 * RoomScale, r\z - 732.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2nuke"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 576.0 * RoomScale, r\y, r\z + 152.0 * RoomScale, 90.0, r, False, 4, 5)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) - 0.09, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) + 0.09, True)
			
			d = CreateDoor(r\Zone, r\x - 544.0 * RoomScale, r\y + 1504.0 * RoomScale, r\z + 738.0 * RoomScale, 90.0, r, False, False, 5)
			d\AutoClose = False : d\Open = False			
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), r\z + 608.0 * RoomScale, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), r\z + 608.0 * RoomScale, True)
			
			; ~ Elevators
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 1192.0 * RoomScale, r\y, r\z, 90.0, r, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True) - 0.031, EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True) + 0.031, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 680.0 * RoomScale, r\y + 1504.0 * RoomScale, r\z, 90.0, r, False, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False
			PositionEntity(r\RoomDoors[1]\Buttons[0], EntityX(r\RoomDoors[1]\Buttons[0], True) - 0.031, EntityY(r\RoomDoors[1]\Buttons[0], True), EntityZ(r\RoomDoors[1]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[1]\Buttons[1], EntityX(r\RoomDoors[1]\Buttons[1], True) + 0.031, EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\Buttons[1], True), True)
			
			; ~ Elevators' pivots
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x + 1496.0 * RoomScale, r\y + 240.0 * RoomScale, r\z)
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x + 984.0 * RoomScale, r\y + 1744.0 * RoomScale, r\z)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 1110.0 * RoomScale, r\y + 36.0 * RoomScale, r\z - 208.0 * RoomScale)
			
			For i = 4 To 6
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For k = 0 To 1
				r\Objects[k * 2] = CopyEntity(o\LeverModelID[0])
				r\Objects[k * 2 + 1] = CopyEntity(o\LeverModelID[1])
				r\Levers[k] = r\Objects[k * 2 + 1]
				
				For i = 0 To 1
					ScaleEntity(r\Objects[k * 2 + i], 0.04, 0.04, 0.04)
					PositionEntity(r\Objects[k * 2 + i], r\x - 975.0 * RoomScale, r\y + 1712.0 * RoomScale, r\z - (502.0 - 132.0 * k) * RoomScale)
					EntityParent(r\Objects[k * 2 + i], r\OBJ)
				Next
				RotateEntity(r\Objects[k * 2], 0.0, -90.0 - 180.0, 0.0)
				RotateEntity(r\Objects[k * 2 + 1], 10.0, -90.0 - 180.0 - 180.0, 0.0)
				EntityPickMode(r\Objects[k * 2 + 1], 1, False)
				EntityRadius(r\Objects[k * 2 + 1], 0.1)
			Next
			
			it = CreateItem("Nuclear Device Document", "paper", r\x - 944.0 * RoomScale, r\y + 1684.0 * RoomScale, r\z - 706.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Ballistic Vest", "vest", r\x - 768.0 * RoomScale, r\y + 1652.0 * RoomScale, r\z - 768.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, -90.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			sc = CreateSecurityCam(r\x + 624.0 * RoomScale, r\y + 1888.0 * RoomScale, r\z - 312.0 * RoomScale, r)
			sc\Angle = 90.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			;[End Block]
		Case "room2tunnel"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 264.0 * RoomScale, r\y, r\z + 656.0 * RoomScale, 90.0, r, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True) + 0.031, EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True) - 0.031, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True)			
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x - 264.0 * RoomScale, r\y, r\z - 656.0 * RoomScale, 270.0, r, True, 3)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = True
			PositionEntity(r\RoomDoors[2]\Buttons[0], EntityX(r\RoomDoors[2]\Buttons[0], True) - 0.031, EntityY(r\RoomDoors[2]\Buttons[0], True), EntityZ(r\RoomDoors[2]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[2]\Buttons[1], EntityX(r\RoomDoors[2]\Buttons[1], True) + 0.031, EntityY(r\RoomDoors[2]\Buttons[1], True), EntityZ(r\RoomDoors[2]\Buttons[1], True), True)
			
			Temp = ((Int(AccessCode) * 3) Mod 10000)
			If Temp < 1000 Then Temp = Temp + 1000
			d = CreateDoor(r\Zone, r\x, r\y, r\z, 0.0, r, False, True, False, Temp)
			PositionEntity(d\Buttons[0], r\x + 230.0 * RoomScale, EntityY(d\Buttons[1], True), r\z - 384.0 * RoomScale, True)
			RotateEntity(d\Buttons[0], 0.0, -90.0, 0.0, True)
			PositionEntity(d\Buttons[1], r\x - 230.0 * RoomScale, EntityY(d\Buttons[1], True), r\z + 384.0 * RoomScale, True)		
			RotateEntity(d\Buttons[1], 0.0, 90.0, 0.0, True)
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 2640.0 * RoomScale, r\y - 2496.0 * RoomScale, r\z + 400.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 4336.0 * RoomScale, r\y - 2496.0 * RoomScale, r\z - 2512.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			RotateEntity(r\Objects[2], 0.0, 180.0, 0.0, True)
			PositionEntity(r\Objects[2], r\x + 552.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 656.0 * RoomScale)
			
			For i = 0 To 2
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x - 552.0 * RoomScale, r\y + 240.0 * RoomScale, r\z - 656.0 * RoomScale)
			EntityParent(r\Objects[4], r\OBJ)
			
			de = CreateDecal(0, r\x + 64.0 * RoomScale, r\y + 0.005, r\z + 144.0 * RoomScale, 90.0, Rand(360.0), 0.0)
			EntityParent(de\OBJ, r\OBJ)
			
			it = CreateItem("Scorched Note", "paper", r\x + 64.0 * RoomScale, r\y + 144.0 * RoomScale, r\z - 384.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "008"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 296.0 * RoomScale, r\y, r\z - 672.0 * RoomScale, 180.0, r, True, False, 4)
			d\AutoClose = False
			PositionEntity(d\Buttons[1], r\x + 164.0 * RoomScale, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
			FreeEntity(d\OBJ2) : d\OBJ2 = 0
			r\RoomDoors[0] = d
			
			d2 = CreateDoor(r\Zone, r\x + 296.0 * RoomScale, r\y, r\z - 144.0 * RoomScale, 0.0, r)
			d2\AutoClose = False
			PositionEntity(d2\Buttons[0], r\x + 432.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z - 480.0 * RoomScale, True)
			RotateEntity(d2\Buttons[0], 0.0, -90.0, 0.0, True)			
			PositionEntity (d2\Buttons[1], r\x + 164.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z - 128.0 * RoomScale, True)
			FreeEntity(d2\OBJ2) : d2\OBJ2 = 0
			r\RoomDoors[1] = d2
			
			d\LinkedDoor = d2
			d2\LinkedDoor = d
			
			d = CreateDoor(r\Zone, r\x - 384.0 * RoomScale, r\y, r\z - 672.0 * RoomScale, 0.0, r, False, False, 4)
			d\AutoClose = False : d\Locked = True : r\RoomDoors[2] = d
			
			; ~ The container
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 292.0 * RoomScale, r\y + 130.0 * RoomScale, r\z + 516.0 * RoomScale, True)
			
			; ~ The lid of the container
			r\Objects[1] = LoadMesh_Strict("GFX\map\008_2.b3d")
			ScaleEntity(r\Objects[1], RoomScale, RoomScale, RoomScale)
			PositionEntity(r\Objects[1], r\x + 292 * RoomScale, r\y + 151.0 * RoomScale, r\z + 576.0 * RoomScale, 0)
			RotateEntity(r\Objects[1], 89.0, 0.0, 0.0, True)
			
			r\Levers[0] = r\Objects[1]
			
			GlassTex = LoadTexture_Strict("GFX\map\glass.png", 1 + 2)
			r\Objects[2] = CreateSprite()
			EntityTexture(r\Objects[2], GlassTex)
			SpriteViewMode(r\Objects[2], 2)
			ScaleSprite(r\Objects[2], 256.0 * RoomScale * 0.5, 194.0 * RoomScale * 0.5)
			PositionEntity(r\Objects[2], r\x - 176.0 * RoomScale, r\y + 224.0 * RoomScale, r\z + 448.0 * RoomScale)
			TurnEntity(r\Objects[2], 0.0, 90.0, 0.0)			
			FreeTexture GlassTex
			
			; ~ SCP-173 spawnpoint
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 445.0 * RoomScale, r\y + 120.0 * RoomScale, r\z + 544.0 * RoomScale)
			
			; ~ SCP-173 attack point
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x + 67.0 * RoomScale, r\y + 120.0 * RoomScale, r\z + 464.0 * RoomScale)
			
			r\Objects[5] = CreateSprite()
			PositionEntity(r\Objects[5], r\x - 158 * RoomScale, r\y + 368 * RoomScale, r\z + 298.0 * RoomScale)
			ScaleSprite(r\Objects[5], 0.02, 0.02)
			EntityTexture(r\Objects[5], LightSpriteTex(1))
			EntityBlend(r\Objects[5], 3)
			HideEntity(r\Objects[5])
			
			; ~ Spawnpoint for the scientist used in the "SCP-008 zombie scene"
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 160.0 * RoomScale, r\y + 672.0 * RoomScale, r\z - 384.0 * RoomScale)
			
			; ~ Spawnpoint for the player
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x, r\y + 672.0 * RoomScale, r\z + 352.0 * RoomScale)
			
			For i = 0 To 7
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Hazmat Suit", "hazmatsuit", r\x - 76.0 * RoomScale, r\y + 0.5, r\z - 396.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, 90.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-008", "paper", r\x - 245.0 * RoomScale, r\y + 192.0 * RoomScale, r\z + 368.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			sc = CreateSecurityCam(r\x + 578.956 * RoomScale, r\y + 444.956 * RoomScale, r\z + 772.0 * RoomScale, r)
			sc\Angle = 135
			sc\Turn = 45
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0, True)
			;[End Block]
		Case "room035"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 296.0 * RoomScale, r\y, r\z - 672.0 * RoomScale, 180.0, r, True, 4, 5)
			d\AutoClose = False : d\Locked = True
			FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
			PositionEntity(d\Buttons[1], r\x - 164.0 * RoomScale, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			r\RoomDoors[0] = d
			
			d2 = CreateDoor(r\Zone, r\x - 296.0 * RoomScale, r\y, r\z - 144.0 * RoomScale, 0.0, r, False, 4)
			d2\AutoClose = False : d2\Locked = True
			PositionEntity(d2\Buttons[0], r\x - 438.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z - 480.0 * RoomScale, True)
			RotateEntity(d2\Buttons[0], 0.0, 90.0, 0.0, True)
			FreeEntity(d2\Buttons[1]) : d2\Buttons[1] = 0
			r\RoomDoors[1] = d2
			
			d\LinkedDoor = d2
			d2\LinkedDoor = d
			
			; ~ Door to the control room
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x + 384.0 * RoomScale, r\y, r\z - 672.0 * RoomScale, 180.0, r, False, False, 5)
			r\RoomDoors[2]\AutoClose = False
			
			; ~ Door to the storage room
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x + 768.0 * RoomScale, r\y, r\z + 512.0 * RoomScale, 90.0, r, False, False, False, "5731")
			r\RoomDoors[3]\AutoClose = False			
			
			For i = 0 To 1
				r\Objects[i * 2] = CopyEntity(o\LeverModelID[0])
				r\Objects[i * 2 + 1] = CopyEntity(o\LeverModelID[1])
				
				r\Levers[i] = r\Objects[i * 2 + 1]
				
				For k = 0 To 1
					ScaleEntity(r\Objects[i * 2 + k], 0.04, 0.04, 0.04)
					PositionEntity (r\Objects[i * 2 + k], r\x + 210.0 * RoomScale, r\y + 224.0 * RoomScale, r\z - (208.0 - i * 76.0) * RoomScale)
					EntityParent(r\Objects[i * 2 + k], r\OBJ)
				Next
				
				RotateEntity(r\Objects[i * 2], 0, -90.0 - 180.0, 0.0)
				RotateEntity(r\Objects[i * 2 + 1], -80.0, -90.0, 0.0)
				EntityPickMode(r\Objects[i * 2 + 1], 1, False)
				EntityRadius(r\Objects[i * 2 + 1], 0.1)	
			Next
			
			; ~ The control room
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x + 456.0 * RoomScale, r\y + 0.5, r\z + 400.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x - 576.0 * RoomScale, r\y + 0.5, r\z + 640.0 * RoomScale)
			
			For i = 3 To 4
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			; ~ The corners of the cont chamber (needed to calculate whether the player is inside the chamber)
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x - 720.0 * RoomScale, r\y + 0.5, r\z + 880.0 * RoomScale)
			
			r\Objects[8] = CreatePivot()
			PositionEntity(r\Objects[8], r\x + 176.0 * RoomScale, r\y + 0.5, r\z - 144.0 * RoomScale)	
			
			For i = 7 To 8
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For i = 0 To 1
				em = CreateEmitter(r\x - 272.0 * RoomScale, r\y + 20.0, r\z + (624.0 - i * 512.0) * RoomScale, 0)
				em\RandAngle = 15.0 : em\Speed = 0.05 : em\SizeChange = 0.007 : em\Achange = -0.006 : em\Gravity = -0.24
				TurnEntity(em\OBJ, 90.0, 0.0, 0.0)
				EntityParent(em\OBJ, r\OBJ)
				r\Objects[5 + i] = em\OBJ
			Next
			
			it = CreateItem("SCP-035 Addendum", "paper", r\x + 248.0 * RoomScale, r\y + 220.0 * RoomScale, r\z + 576.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Radio Transceiver", "radio", r\x - 544.0 * RoomScale, r\y + 0.5, r\z + 704.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("SCP-500-01", "scp500", r\x + 1168.0 * RoomScale, r\y + 250.0 * RoomScale, r\z + 576 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Metal Panel", "scp148", r\x - 360.0 * RoomScale, r\y + 0.5, r\z + 644.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-035", "paper", r\x + 1168.0 * RoomScale, r\y + 100.0 * RoomScale, r\z + 408.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room513"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 704.0 * RoomScale, r\y, r\z + 304.0 * RoomScale, 0.0, r, False, False, 3)
			d\AutoClose = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) + 0.061, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.031, True)
			
			sc = CreateSecurityCam(r\x - 312.0 * RoomScale, r\y + 414.0 * RoomScale, r\z + 656.0 * RoomScale, r)
			sc\FollowPlayer = True
			
			it = CreateItem("SCP-513", "scp513", r\x - 32.0 * RoomScale, r\y + 196.0 * RoomScale, r\z + 688.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Blood-stained Note", "paper", r\x + 736.0 * RoomScale, r\y + 1.0, r\z + 48.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-513", "paper", r\x - 480.0 * RoomScale, r\y + 104.0 * RoomScale, r\z - 176.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room966"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 400.0 * RoomScale, r\y, r\z, -90.0, r, False, False, 3)
			
			d = CreateDoor(r\Zone, r\x, r\y, r\z - 480.0 * RoomScale, 180.0, r, False, False, 3)
			
			sc = CreateSecurityCam(r\x - 312.0 * RoomScale, r\y + 452.0 * RoomScale, r\z + 644.0 * RoomScale, r)
			sc\Angle = 225.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x, r\y + 0.5, r\z + 512.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 64.0 * RoomScale, r\y + 0.5, r\z - 640.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x, r\y + 0.5, r\z)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x + 320.0 * RoomScale, r\y + 0.5, r\z + 704.0 * RoomScale)
			
			For i = 0 To 3
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Night Vision Goggles", "nvgoggles", r\x + 320.0 * RoomScale, r\y + 0.5, r\z + 704.0 * RoomScale)
			it\State = 300.0
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room3storage"
			;[Block]
			; ~ Elevator Doors
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x, r\y, r\z + 448.0 * RoomScale, 180.0, r, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 5840.0 * RoomScale, r\y - 5632.0 * RoomScale, r\z + 1048.0 * RoomScale, 180.0, r, False, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False
			PositionEntity(r\RoomDoors[1]\Buttons[0], EntityX(r\RoomDoors[1]\Buttons[0], True), EntityY(r\RoomDoors[1]\Buttons[0], True), EntityZ(r\RoomDoors[1]\Buttons[0], True) + 0.031, True)					
			PositionEntity(r\RoomDoors[1]\Buttons[1], EntityX(r\RoomDoors[1]\Buttons[1], True), EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\Buttons[1], True) - 0.031, True)
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x + 608.0 * RoomScale, r\y, r\z - 312.0 * RoomScale, 0.0, r, True, 3)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = True
			PositionEntity(r\RoomDoors[2]\Buttons[0], EntityX(r\RoomDoors[2]\Buttons[0], True), EntityY(r\RoomDoors[2]\Buttons[0], True), EntityZ(r\RoomDoors[2]\Buttons[0], True) - 0.031, True)					
			PositionEntity(r\RoomDoors[2]\Buttons[1], EntityX(r\RoomDoors[2]\Buttons[1], True), EntityY(r\RoomDoors[2]\Buttons[1], True), EntityZ(r\RoomDoors[2]\Buttons[1], True) + 0.031, True)
			
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x - 456.0 * RoomScale, r\y - 5632.0 * RoomScale, r\z - 824.0 * RoomScale, 0.0, r, False, 3)
			r\RoomDoors[3]\AutoClose = False : r\RoomDoors[3]\Open = False
			PositionEntity(r\RoomDoors[3]\Buttons[0], EntityX(r\RoomDoors[3]\Buttons[0], True), EntityY(r\RoomDoors[3]\Buttons[0], True), EntityZ(r\RoomDoors[3]\Buttons[0], True) - 0.031, True)					
			PositionEntity(r\RoomDoors[3]\Buttons[1], EntityX(r\RoomDoors[3]\Buttons[1], True), EntityY(r\RoomDoors[3]\Buttons[1], True), EntityZ(r\RoomDoors[3]\Buttons[1], True) + 0.031, True)
			
			; ~ Other doors
			r\RoomDoors[4] = CreateDoor(r\Zone, r\x + 56.0 * RoomScale, r\y - 5632.0 * RoomScale, r\z + 6344.0 * RoomScale, 90.0, r, False, 2)
			r\RoomDoors[4]\AutoClose = False : r\RoomDoors[4]\Open = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[4]\Buttons[i]) : r\RoomDoors[4]\Buttons[i] = 0
			Next
			
			d = CreateDoor(r\Zone, r\x + 1157.0 * RoomScale, r\y - 5632.0 * RoomScale, r\z + 660.0 * RoomScale, 0.0, r, False, 2)
			d\Locked = True : d\Open = False : d\AutoClose = False
			
			For i = 0 To 1
				FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
			Next
			
			d = CreateDoor(r\Zone, r\x + 234.0 * RoomScale, r\y - 5632.0 * RoomScale, r\z + 5239.0 * RoomScale, 90.0, r, False, 2)
			d\Locked = True : d\Open = False : d\AutoClose = False
			
			For i = 0 To 1
				FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
			Next
			
			d = CreateDoor(r\Zone, r\x + 3446.0 * RoomScale, r\y - 5632.0 * RoomScale, r\z + 6369.0 * RoomScale, 90.0, r, False, 2)
			d\Locked = True : d\Open = False : d\AutoClose = False
			
			For i = 0 To 1
				FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
			Next
			
			; ~ Elevators' pivots
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x, r\y + 240.0 * RoomScale, r\z + 752.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 5840.0 * RoomScale, r\y - 5392.0 * RoomScale, r\z + 1360.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x + 608.0 * RoomScale, r\y + 240.0 * RoomScale, r\z - 624.0 * RoomScale)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 456.0 * RoomScale, r\y - 5392.0 * RoomScale, r\z - 1136 * RoomScale)
			
			; ~ Waypoints # 1
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x + 2128.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 2048.0 * RoomScale)
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x + 2128.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z - 1136.0 * RoomScale)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 3824.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z - 1168.0 * RoomScale)
			
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x + 3760.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 2048.0 * RoomScale)
			
			r\Objects[8] = CreatePivot()
			PositionEntity(r\Objects[8], r\x + 4848.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 112.0 * RoomScale)
			
			; ~ Waypoints # 2
			r\Objects[9] = CreatePivot()
			PositionEntity(r\Objects[9], r\x + 592.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 6352.0 * RoomScale)
			
			r\Objects[10] = CreatePivot()
			PositionEntity(r\Objects[10], r\x + 2928.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 6352.0 * RoomScale)
			
			r\Objects[11] = CreatePivot()
			PositionEntity(r\Objects[11], r\x + 2928.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 5200.0 * RoomScale)
			
			r\Objects[12] = CreatePivot()
			PositionEntity(r\Objects[12], r\x + 592.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 5200.0 * RoomScale)
			
			; ~ Waypoints # 3
			r\Objects[13] = CreatePivot()
			PositionEntity(r\Objects[13], r\x + 1136.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 2944.0 * RoomScale)
			
			r\Objects[14] = CreatePivot()
			PositionEntity(r\Objects[14], r\x + 1104.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 1184.0 * RoomScale)
			
			r\Objects[15] = CreatePivot()
			PositionEntity(r\Objects[15], r\x - 464.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 1216.0 * RoomScale)
			
			r\Objects[16] = CreatePivot()
			PositionEntity(r\Objects[16], r\x - 432.0 * RoomScale, r\y - 5550.0 * RoomScale, r\z + 2976.0 * RoomScale)
			
			For i = 0 To 16
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[20] = LoadMesh_Strict("GFX\map\room3storage_hb.b3d", r\OBJ)
			EntityPickMode(r\Objects[20], 2)
			EntityType(r\Objects[20], HIT_MAP)
			EntityAlpha(r\Objects[20], 0.0)
			
			em = CreateEmitter(r\x + 5218.0 * RoomScale, r\y - 5584.0 * RoomScale, r\z - 600.0 * RoomScale, 0)
			em\RandAngle = 15 : em\Speed = 0.03 : em\SizeChange = 0.01 : em\Achange = -0.006 : em\Gravity = -0.2 
			TurnEntity(em\OBJ, 20.0, -100.0, 0.0)
			EntityParent(em\OBJ, r\OBJ) : em\room = r
			
			Select Rand(3)
				Case 1
					;[Block]
					iX = 2312
					iZ = -952
					;[End Block]
				Case 2
					;[Block]
					iX = 3032
					iZ = 1288
					;[End Block]
				Case 3
					;[Block]
					iX = 2824
					iZ = 2808
					;[End Block]
			End Select
			
			it = CreateItem("Black Severed Hand", "hand2", r\x + iX * RoomScale, r\y - 5596.0 * RoomScale + 1.0, r\z + iZ * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Night Vision Goggles", "nvgoggles", r\x + 1936.0 * RoomScale, r\y - 5496.0 * RoomScale, r\z - 944.0 * RoomScale)
			it\State = 450.0
			EntityParent(it\Collider, r\OBJ)
			
			de = CreateDecal(3, r\x + iX * RoomScale, r\y - 5632.0 * RoomScale + 0.01, r\z + iZ * RoomScale, 90.0, Rnd(360.0), 0.0)
			de\Size = 0.5
			ScaleSprite(de\OBJ, de\Size, de\Size)
			EntityParent(de\OBJ, r\OBJ)
			
			For k = 10 To 11
				r\Objects[k * 2] = CopyEntity(o\LeverModelID[0])
				r\Objects[k * 2 + 1] = CopyEntity(o\LeverModelID[1])
				
				r\Levers[k - 10] = r\Objects[k * 2 + 1]
				
				For i = 0 To 1
					ScaleEntity(r\Objects[k * 2 + i], 0.04, 0.04, 0.04)
					If k = 10
						PositionEntity(r\Objects[k * 2 + i], r\x + 3095.5 * RoomScale, r\y - 5461.0 * RoomScale, r\z + 6568.0 * RoomScale)
					Else
						PositionEntity(r\Objects[k * 2 + i], r\x + 1215.5 * RoomScale, r\y - 5461.0 * RoomScale, r\z + 3164.0 * RoomScale)
					EndIf
					EntityParent(r\Objects[k * 2 + i], r\OBJ)
				Next
				RotateEntity(r\Objects[k * 2], 0.0, 0.0, 0.0)
				RotateEntity(r\Objects[k * 2 + 1], -10.0, 0.0 - 180.0, 0.0)
				EntityPickMode(r\Objects[k * 2 + 1], 1, False)
				EntityRadius(r\Objects[k * 2 + 1], 0.1)
			Next
			;[End Block]
		Case "room049"
			;[Block]
			; ~ Elevator doors
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 328.0 * RoomScale, r\y, r\z + 656.0 * RoomScale, 90.0, r, True, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True) + 0.031, EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True) - 0.031, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True)	
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 2908.0 * RoomScale, r\y - 3520.0 * RoomScale, r\z + 1824.0 * RoomScale, 90.0, r, False, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False	
			PositionEntity(r\RoomDoors[1]\Buttons[0], EntityX(r\RoomDoors[1]\Buttons[0], True) - 0.018, EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[1]\Buttons[1], EntityX(r\RoomDoors[1]\Buttons[1], True) + 0.018, EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\Buttons[1], True), True)	
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x - 672.0 * RoomScale, r\y, r\z - 408.0 * RoomScale, 180.0, r, True, 3)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = True
			PositionEntity(r\RoomDoors[2]\Buttons[0], EntityX(r\RoomDoors[2]\Buttons[0], True), EntityY(r\RoomDoors[2]\Buttons[1], True), EntityZ(r\RoomDoors[2]\Buttons[0], True) + 0.031, True)
			PositionEntity(r\RoomDoors[2]\Buttons[1], EntityX(r\RoomDoors[2]\Buttons[1], True), EntityY(r\RoomDoors[2]\Buttons[1], True), EntityZ(r\RoomDoors[2]\Buttons[1], True) - 0.031, True)				
			
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x - 2766.0 * RoomScale, r\y - 3520.0 * RoomScale, r\z - 1592.0 * RoomScale, 180.0, r, False, 3)
			r\RoomDoors[3]\AutoClose = False : r\RoomDoors[3]\Open = False		
			PositionEntity(r\RoomDoors[3]\Buttons[0], EntityX(r\RoomDoors[3]\Buttons[0], True), EntityY(r\RoomDoors[3]\Buttons[1], True), EntityZ(r\RoomDoors[3]\Buttons[0], True) + 0.031, True)
			PositionEntity(r\RoomDoors[3]\Buttons[1], EntityX(r\RoomDoors[3]\Buttons[1], True), EntityY(r\RoomDoors[3]\Buttons[1], True), EntityZ(r\RoomDoors[3]\Buttons[1], True) - 0.031, True)
			
			; ~ Storage room doors
			r\RoomDoors[4] = CreateDoor(r\Zone, r\x + 272.0 * RoomScale, r\y - 3552.0 * RoomScale, r\z + 104.0 * RoomScale, 90.0, r)
			r\RoomDoors[4]\AutoClose = False : r\RoomDoors[4]\Open = True : r\RoomDoors[4]\Locked = True
			
			r\RoomDoors[5] = CreateDoor(r\Zone, r\x + 264.0 * RoomScale, r\y - 3520.0 * RoomScale, r\z - 1824.0 * RoomScale, 90.0, r)
			r\RoomDoors[5]\AutoClose = False : r\RoomDoors[5]\Open = True : r\RoomDoors[5]\Locked = True
			PositionEntity(r\RoomDoors[5]\Buttons[0], EntityX(r\RoomDoors[5]\Buttons[0], True) - 0.031, EntityY(r\RoomDoors[5]\Buttons[1], True), EntityZ(r\RoomDoors[5]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[5]\Buttons[1], EntityX(r\RoomDoors[5]\Buttons[1], True) + 0.031, EntityY(r\RoomDoors[5]\Buttons[1], True), EntityZ(r\RoomDoors[5]\Buttons[1], True), True)					
			
			r\RoomDoors[6] = CreateDoor(r\Zone, r\x - 264.0 * RoomScale, r\y - 3520.0 * RoomScale, r\z + 1824.0 * RoomScale, 90.0, r)
			r\RoomDoors[6]\AutoClose = False : r\RoomDoors[6]\Open = True : r\RoomDoors[6]\Locked = True
			PositionEntity(r\RoomDoors[6]\Buttons[0], EntityX(r\RoomDoors[6]\Buttons[0], True) - 0.031, EntityY(r\RoomDoors[6]\Buttons[1], True), EntityZ(r\RoomDoors[6]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[6]\Buttons[1], EntityX(r\RoomDoors[6]\Buttons[1], True) + 0.031, EntityY(r\RoomDoors[6]\Buttons[1], True), EntityZ(r\RoomDoors[6]\Buttons[1], True), True)
			
			; ~ DNA door
			d = CreateDoor(r\Zone, r\x, r\y, r\z, 0.0, r, False, 2, -2)
			d\AutoClose = False
			
			; ~ Other doors
			d = CreateDoor(r\Zone, r\x - 272.0 * RoomScale, r\y - 3552.0 * RoomScale, r\z + 98.0 * RoomScale, 90.0, r, True, True)
			d\AutoClose = False : d\Open = True : d\MTFClose = False : d\Locked = True
			
			For i = 0 To 1
				FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
			Next
			
			d = CreateDoor(r\Zone, r\x - 2990.0 * RoomScale, r\y - 3520.0 * RoomScale, r\z - 1824.0 * RoomScale, 90.0, r, False, 2)
			d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			d = CreateDoor(r\Zone, r\x - 896.0 * RoomScale, r\y, r\z - 640 * RoomScale, 90.0, r, False, 2)
			d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			; ~ Elevators' pivots
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 640.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 656.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 3211.0 * RoomScale, r\y - 3280.0 * RoomScale, r\z + 1824.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x - 672.0 * RoomScale, r\y + 240.0 * RoomScale, r\z - 93.0 * RoomScale)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 2766.0 * RoomScale, r\y - 3280.0 * RoomScale, r\z - 1277.0 * RoomScale)
			
			; ~ Zombie # 1
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x + 528.0 * RoomScale, r\y - 3440.0 * RoomScale, r\z + 96.0 * RoomScale)
			
			; ~ Zombie # 2
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x  + 64.0 * RoomScale, r\y - 3440.0 * RoomScale, r\z - 1000.0 * RoomScale)
			
			For i = 0 To 5
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For k = 0 To 1
				r\Objects[k * 2 + 6] = CopyEntity(o\LeverModelID[0])
				r\Objects[k * 2 + 7] = CopyEntity(o\LeverModelID[1])
				
				r\Levers[k] = r\Objects[k * 2 + 7]
				
				For i = 0 To 1
					ScaleEntity(r\Objects[k * 2 + 6 + i], 0.03, 0.03, 0.03)
					
					Select k
						Case 0 ; ~ Power feed
							;[Block]
							PositionEntity(r\Objects[k * 2 + 6 + i], r\x + 852.0 * RoomScale, r\y - 3374.0 * RoomScale, r\z - 854.0 * RoomScale)
							;[End Block]
						Case 1 ; ~ Generator
							;[Block]
							PositionEntity(r\Objects[k * 2 + 6 + i], r\x - 834.0 * RoomScale, r\y - 3400.0 * RoomScale, r\z + 1093.0 * RoomScale)
							;[End Block]
					End Select
					EntityParent(r\Objects[k * 2 + 6 + i], r\OBJ)
				Next
				RotateEntity(r\Objects[k * 2 + 6], 0.0, 180.0 + 90.0 * (Not k), 0.0)
				RotateEntity(r\Objects[k * 2 + 7], 81.0 - 92.0 * k, 90.0 * (Not k), 0.0)
				EntityPickMode(r\Objects[k * 2 + 7], 1, False)
				EntityRadius(r\Objects[k * 2 + 7], 0.1)
			Next
			
			r\Objects[10] = CreatePivot()
			PositionEntity(r\Objects[10], r\x - 832.0 * RoomScale, r\y - 3484.0 * RoomScale, r\z + 1572.0 * RoomScale)
			
			; ~ Spawnpoint for the map layout document
			r\Objects[11] = CreatePivot()
			PositionEntity(r\Objects[11], r\x + 2642.0 * RoomScale, r\y - 3516.0 * RoomScale, r\z + 1822.0 * RoomScale)
			
			r\Objects[12] = CreatePivot()
			PositionEntity(r\Objects[12], r\x - 2666.0 * RoomScale, r\y - 3516.0 * RoomScale, r\z - 1792.0 * RoomScale)
			
			For i = 10 To 12
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Document SCP-049", "paper", r\x - 608.0 * RoomScale, r\y - 3332.0 * RoomScale, r\z + 876.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Level 4 Key Card", "key4", r\x - 512.0 * RoomScale, r\y - 3412.0 * RoomScale, r\z + 864.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("First Aid Kit", "firstaid", r\x + 385.0 * RoomScale, r\y - 3412.0 * RoomScale, r\z + 271.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2_2"
			;[Block]
			For r2.Rooms = Each Rooms
				If r2 <> r Then
					If r2\RoomTemplate\Name = "room2_2" Then
						r\Objects[0] = CopyEntity(r2\Objects[0]) ; ~ Don't load the mesh again
						Exit
					EndIf
				EndIf
			Next
			If r\Objects[0] = 0 Then r\Objects[0] = LoadMesh_Strict("GFX\map\fan.b3d")
			ScaleEntity(r\Objects[0], RoomScale, RoomScale, RoomScale)
			PositionEntity(r\Objects[0], r\x - 248.0 * RoomScale, r\y + 528.0 * RoomScale, r\z)
			EntityParent(r\Objects[0], r\OBJ)
			;[End Block]
		Case "room012"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 264.0 * RoomScale, r\y, r\z + 672.0 * RoomScale, 270.0, r, False, False, 3)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) + 0.061, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.031, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.061, True)
			
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 512.0 * RoomScale, r\y - 768.0 * RoomScale, r\z - 336.0 * RoomScale, 0.0, r)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False : r\RoomDoors[0]\Locked = True
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x + 176.0 * RoomScale, r\y - 512.0 * RoomScale, r\z - 352.0 * RoomScale, True)
			FreeEntity(r\RoomDoors[0]\Buttons[1]) : r\RoomDoors[0]\Buttons[1] = 0
			
			r\Objects[0] = CopyEntity(o\LeverModelID[0])
			r\Objects[1] = CopyEntity(o\LeverModelID[1])
			r\Levers[0] = r\Objects[1]
			
			For i = 0 To 1
				ScaleEntity(r\Objects[i], 0.04, 0.04, 0.04)
				PositionEntity(r\Objects[i], r\x + 240.0 * RoomScale, r\y - 512.0 * RoomScale, r\z - 364 * RoomScale, True)
				EntityParent(r\Objects[i], r\OBJ)
			Next
			RotateEntity(r\Objects[1], 10.0, -180.0, 0.0)
			
			EntityPickMode(r\Objects[1], 1, False)
			EntityRadius(r\Objects[1], 0.1)
			
			r\Objects[2] = LoadMesh_Strict("GFX\map\room012_2.b3d")
			ScaleEntity(r\Objects[2], RoomScale, RoomScale, RoomScale)
			PositionEntity(r\Objects[2], r\x - 360 * RoomScale, - 130.0 * RoomScale, r\z + 456.0 * RoomScale)
			
			r\Objects[3] = CreateSprite()
			PositionEntity(r\Objects[3], r\x - 43.5 * RoomScale, - 574.0 * RoomScale, r\z - 362.0 * RoomScale)
			ScaleSprite(r\Objects[3], 0.015, 0.015)
			EntityTexture(r\Objects[3], LightSpriteTex(1))
			EntityBlend(r\Objects[3], 3)
			HideEntity(r\Objects[3])
			
			For i = 2 To 3
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[4] = LoadMesh_Strict("GFX\map\room012_3.b3d")
			Tex = LoadTexture_Strict("GFX\map\scp-012_0.jpg")
			EntityTexture(r\Objects[4], Tex, 0, 1)
			ScaleEntity(r\Objects[4], RoomScale, RoomScale, RoomScale)
			PositionEntity(r\Objects[4], r\x - 360.0 * RoomScale, r\y - 130.0 * RoomScale, r\z + 456.0 * RoomScale)
			EntityParent(r\Objects[4], r\Objects[2])
			
			it = CreateItem("Document SCP-012", "paper", r\x - 56.0 * RoomScale, r\y - 576.0 * RoomScale, r\z - 408.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Severed Hand", "hand", r\x - 784.0 * RoomScale, r\y - 576.0 * RoomScale + 0.3, r\z + 640.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			de = CreateDecal(3, r\x - 784.0 * RoomScale, r\y - 768.0 * RoomScale + 0.01, r\z + 640.0 * RoomScale, 90.0, Rnd(360.0), 0.0)
			de\Size = 0.5
			ScaleSprite(de\OBJ, de\Size, de\Size)
			EntityParent(de\OBJ, r\OBJ)
			;[End Block]
		Case "tunnel2"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x, 544.0 * RoomScale, r\z + 512.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x, 544.0 * RoomScale, r\z - 512.0 * RoomScale)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room2pipes"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 368.0 * RoomScale, r\y, r\z)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 368.0 * RoomScale, r\y, r\z)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x + 224.0 * RoomScale - 0.005, r\y + 192.0 * RoomScale, r\z)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 224.0 * RoomScale + 0.005, r\y + 192.0 * RoomScale, r\z)
			
			For i = 0 To 3
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room3pit"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 704.0 * RoomScale, r\y + 112.0 * RoomScale, r\z - 416.0 * RoomScale)
			EntityParent(r\Objects[i], r\OBJ)
			
			em = CreateEmitter(r\x + 512.0 * RoomScale, r\y - 76.0 * RoomScale, r\z - 688.0 * RoomScale, 0)
			em\RandAngle = 55.0 : em\Speed = 0.0005 : em\Achange = -0.015 : em\SizeChange = 0.007
			TurnEntity(em\OBJ, -90.0, 0.0, 0.0)
			EntityParent(em\OBJ, r\OBJ)
			
			em = CreateEmitter(r\x - 512.0 * RoomScale, r\y - 76.0 * RoomScale, r\z - 688.0 * RoomScale, 0)
			em\RandAngle = 55.0 : em\Speed = 0.0005 : em\Achange = -0.015 : em\SizeChange = 0.007
			TurnEntity(em\OBJ, -90.0, 0.0, 0.0)
			EntityParent(em\OBJ, r\OBJ)
			;[End Block]
		Case "room2servers"
			;[Block]
			; ~ Locked room at the room's center
			d = CreateDoor(r\Zone, r\x, r\y, r\z, 0.0, r, False, 2)
			d\Locked = True
			
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 208.0 * RoomScale, r\y, r\z - 736.0 * RoomScale, 90.0, r, True, False, False, "", True)
			r\RoomDoors[0]\AutoClose = False
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True) - 0.061, EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True) + 0.061, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x - 208.0 * RoomScale, r\y, r\z + 736.0 * RoomScale, 90.0, r, True, False, False, "", True)
			r\RoomDoors[1]\AutoClose = False
			PositionEntity(r\RoomDoors[1]\Buttons[0], EntityX(r\RoomDoors[1]\Buttons[0], True) - 0.061, EntityY(r\RoomDoors[1]\Buttons[0], True), EntityZ(r\RoomDoors[1]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[1]\Buttons[1], EntityX(r\RoomDoors[1]\Buttons[1], True) + 0.061, EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\Buttons[1], True), True)
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x - 672.0 * RoomScale, r\y, r\z - 1024.0 * RoomScale, 0.0, r)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\DisableWaypoint = True : r\RoomDoors[2]\MTFClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[2]\Buttons[i]) : r\RoomDoors[2]\Buttons[i] = 0
			Next
			
			For k = 0 To 2
				r\Objects[k * 2] = CopyEntity(o\LeverModelID[0])
				r\Objects[k * 2 + 1] = CopyEntity(o\LeverModelID[1])
				
				r\Levers[k] = r\Objects[k * 2 + 1]
				
				For i = 0 To 1
					ScaleEntity(r\Objects[k * 2 + i], 0.03, 0.03, 0.03)
					
					Select k
						Case 0 ; ~ Power switch
							;[Block]
							PositionEntity(r\Objects[k * 2 + i], r\x - 1260.0 * RoomScale, r\y + 234.0 * RoomScale, r\z + 750.0 * RoomScale)	
							;[End Block]
						Case 1 ; ~ Generator fuel pump
							;[Block]
							PositionEntity(r\Objects[k * 2 + i], r\x - 920.0 * RoomScale, r\y + 164.0 * RoomScale, r\z + 898.0 * RoomScale)
							;[End Block]
						Case 2 ; ~ Generator On / Off
							;[Block]
							PositionEntity(r\Objects[k * 2 + i], r\x - 837.0 * RoomScale, r\y + 152.0 * RoomScale, r\z + 886.0 * RoomScale)
							;[End Block]
					End Select
					EntityParent(r\Objects[k * 2 + i], r\OBJ)
				Next
				RotateEntity(r\Objects[k * 2 + 1], 81, -180, 0)
				EntityPickMode(r\Objects[k * 2 + 1], 1, False)
				EntityRadius(r\Objects[k * 2 + 1], 0.1)
			Next
			RotateEntity(r\Objects[2 + 1], -81.0, -180.0, 0.0)
			RotateEntity(r\Objects[4 + 1], -81.0, -180.0, 0.0)
			
			; ~ SCP-096's spawnpoint
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x - 320 * RoomScale, r\y + 0.5, r\z)
			
			; ~ Guard's spawnpoint
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x - 1328.0 * RoomScale, r\y + 0.5, r\z + 528.0 * RoomScale)
			
			; ~ The point where the guard walks to
			r\Objects[8] = CreatePivot() 
			PositionEntity(r\Objects[8], r\x - 1376.0 * RoomScale, r\y + 0.5, r\z + 32.0 * RoomScale)
			
			r\Objects[9] = CreatePivot()
			PositionEntity(r\Objects[9], r\x - 848.0 * RoomScale, r\y + 0.5, r\z + 576.0 * RoomScale)
			
			r\Objects[10] = CreatePivot()
			PositionEntity(r\Objects[10], r\x - 700.0 * RoomScale, r\y + 0.5, r\z)
			
			For i = 6 To 10
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "room3servers"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 736.0 * RoomScale, r\y - 512.0 * RoomScale, r\z - 400.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 552.0 * RoomScale, r\y - 512.0 * RoomScale, r\z - 528.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x + 736.0 * RoomScale, r\y - 512.0 * RoomScale, r\z + 272.0 * RoomScale)
			
			r\Objects[3] = CopyEntity(o\NPCModelID[25])
			ScaleEntity(r\Objects[3], 0.07, 0.07, 0.07)
			Tex = LoadTexture_Strict("GFX\npcs\duck(2).png")
			EntityTexture(r\Objects[3], Tex)
			PositionEntity(r\Objects[3], r\x + 928.0 * RoomScale, r\y - 640.0 * RoomScale, r\z + 704.0 * RoomScale)
			
			For i = 0 To 3
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("9V Battery", "bat", r\x - 132.0 * RoomScale, r\y - 368.0 * RoomScale, r\z - 648.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			If Rand(2) = 1 Then
				it = CreateItem("9V Battery", "bat", r\x - 76.0 * RoomScale, r\y - 368.0 * RoomScale, r\z - 648.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)
			EndIf
			
			If Rand(2) = 1 Then
				it = CreateItem("9V Battery", "bat", r\x - 196.0 * RoomScale, r\y - 368.0 * RoomScale, r\z - 648.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)
			EndIf
			
			it = CreateItem("S-NAV 300 Navigator", "nav", r\x + 124.0 * RoomScale, r\y - 368.0 * RoomScale, r\z - 648.0 * RoomScale)
			it\State = 20.0
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room3servers2"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 504.0 * RoomScale, -512.0 * RoomScale, r\z + 271.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 628.0 * RoomScale, -512.0 * RoomScale, r\z + 271.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x - 532.0 * RoomScale, -512.0 * RoomScale, r\z - 877.0 * RoomScale)	
			
			For i = 0 To 2
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Document SCP-970", "paper", r\x + 960.0 * RoomScale, r\y - 448.0 * RoomScale, r\z + 251.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle, 0.0)	
			EntityParent(it\Collider, r\OBJ)	
			
			it = CreateItem("Gas Mask", "gasmask", r\x + 954.0 * RoomScale, r\y - 504.0 * RoomScale, r\z + 235.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)		
			;[End Block]
		Case "room2testroom"
			;[Block]
			; ~ DNA door
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 720.0 * RoomScale, r\y, r\z, 0.0, r, False, 2, -1)
			r\RoomDoors[0]\AutoClose = False
			
			; ~ Door to the center
			d = CreateDoor(r\Zone, r\x - 624.0 * RoomScale, r\y - 1280.0 * RoomScale, r\z, 90.0, r, True)	
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.031, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)			
			
			For xTemp = 0 To 1
				For zTemp = -1 To 1
					r\Objects[xTemp * 3 + (zTemp + 1)] = CreatePivot()
					PositionEntity(r\Objects[xTemp * 3 + (zTemp + 1)], r\x + (-236.0 + 280.0 * xTemp) * RoomScale, r\y - 700.0 * RoomScale, r\z + 384.0 * zTemp * RoomScale)
					EntityParent(r\Objects[xTemp * 3 + (zTemp + 1)], r\OBJ)
				Next
			Next
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 754.0 * RoomScale, r\y - 1248.0 * RoomScale, r\z)
			EntityParent(r\Objects[6], r\OBJ)
			
			sc = CreateSecurityCam(r\x + 744.0 * RoomScale, r\y - 856.0 * RoomScale, r\z + 236.0 * RoomScale, r)
			sc\FollowPlayer = True
			
			it = CreateItem("Document SCP-682", "paper", r\x + 656.0 * RoomScale, r\y - 1200.0 * RoomScale, r\z - 16.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2closets"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 240.0 * RoomScale, r\y - 0.1 * RoomScale, r\z, 90.0, r, False)
			d\Open = False : d\AutoClose = False
			PositionEntity(d\Buttons[0], r\x - 230.0 * RoomScale, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], r\x - 250.0 * RoomScale, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 1120.0 * RoomScale, r\y - 256.0 * RoomScale, r\z + 896.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 1232.0 * RoomScale, r\y - 256.0 * RoomScale, r\z - 160.0 * RoomScale)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			sc = CreateSecurityCam(r\x, r\y + 704.0 * RoomScale, r\z + 863.0 * RoomScale, r)
			sc\Angle = 180.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			
			it = CreateItem("Document SCP-1048", "paper", r\x + 736.0 * RoomScale, r\y + 176.0 * RoomScale, r\z + 736.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Gas Mask", "gasmask", r\x + 736.0 * RoomScale, r\y + 176.0 * RoomScale, r\z + 544.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("9V Battery", "bat", r\x + 736.0 * RoomScale, r\y + 176.0 * RoomScale, r\z - 448.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			If Rand(2) = 1 Then
				it = CreateItem("9V Battery", "bat", r\x + 730.0 * RoomScale, r\y + 176.0 * RoomScale, r\z - 496.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)
			EndIf
			
			If Rand(2) = 1 Then
				it = CreateItem("9V Battery", "bat", r\x + 740.0 * RoomScale, r\y + 176.0 * RoomScale, r\z - 560.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)
			EndIf
			
			it = CreateItem("Level 1 Key Card", "key1", r\x + 736.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 752.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			Local clipboard.Items = CreateItem("Clipboard","clipboard", r\x + 736.0 * RoomScale, r\y + 224.0 * RoomScale, r\z - 480.0 * RoomScale)
			
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Incident Report SCP-1048-A", "paper", r\x + 736.0 * RoomScale, r\y + 224.0 * RoomScale, r\z -480.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ) : HideEntity(it\Collider)
			;[End Block]
		Case "room2offices"
			;[Block]
			w.WayPoints = CreateWaypoint(r\x - 32.0 * RoomScale, r\y + 66.0 * RoomScale, r\z + 288.0 * RoomScale, Null, r)
			w2.WayPoints = CreateWaypoint(r\x, r\y + 66.0 * RoomScale, r\z - 448.0 * RoomScale, Null, r)
			w\Connected[0] = w2 : w\Dist[0] = EntityDistance(w\OBJ, w2\OBJ)
			w2\Connected[0] = w : w2\Dist[0] = w\Dist[0]
			
			it = CreateItem("Document SCP-106", "paper", r\x + 404.0 * RoomScale, r\y + 145.0 * RoomScale, r\z + 559.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Level 2 Key Card", "key2", r\x - 156.0 * RoomScale, r\y + 151.0 * RoomScale, r\z + 72.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("S-NAV 300 Navigator", "nav", r\x + 305.0 * RoomScale, r\y + 153.0 * RoomScale, r\z + 944.0 * RoomScale)
			it\State = 20.0
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Notification", "paper", r\x - 137.0 * RoomScale, r\y + 153.0 * RoomScale, r\z + 464.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2offices2"
			;[Block]
			r\Objects[0] = CopyEntity(o\NPCModelID[25])
			ScaleEntity(r\Objects[0], 0.07, 0.07, 0.07)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 808.0 * RoomScale, r\y - 72.0 * RoomScale, r\z - 40.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x - 488.0 * RoomScale, r\y + 160.0 * RoomScale, r\z + 700.0 * RoomScale)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 488.0 * RoomScale, r\y + 160.0 * RoomScale, r\z - 668.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x - 572.0 * RoomScale, r\y + 350.0 * RoomScale, r\z - 4.0 * RoomScale)
			
			Temp = Rand(1, 4)
			PositionEntity(r\Objects[0], EntityX(r\Objects[Temp], True), EntityY(r\Objects[Temp], True), EntityZ(r\Objects[Temp], True), True)
			
			For i = 0 To 4
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Level 1 Key Card", "key1", r\x - 368.0 * RoomScale, r\y - 48.0 * RoomScale, r\z + 80.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-895", "paper", r\x - 800.0 * RoomScale, r\y - 48.0 * RoomScale, r\z + 368.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			If Rand(2) = 1 Then
				it = CreateItem("Document SCP-860", "paper", r\x - 800.0 * RoomScale, r\y - 48.0 * RoomScale, r\z - 464.0 * RoomScale)
			Else
				it = CreateItem("SCP-093 Recovered Materials", "paper", r\x - 800.0 * RoomScale, r\y - 48.0 * RoomScale, r\z - 464.0 * RoomScale)
			EndIf
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("S-NAV 300 Navigator", "nav", r\x - 336.0 * RoomScale, r\y - 48.0 * RoomScale, r\z - 480.0 * RoomScale)
			it\State = 28.0
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2offices3"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 1056.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 290.0 * RoomScale, 90.0, r, True)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True
			
			If Rand(2) = 1 Then 
				it = CreateItem("Mobile Task Forces", "paper", r\x + 744.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 944.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)	
			Else
				it = CreateItem("Security Clearance Levels", "paper", r\x + 680.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 944.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)			
			EndIf
			
			it = CreateItem("Object Classes", "paper", r\x + 160.0 * RoomScale, r\y + 240.0 * RoomScale, r\z + 568.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)	
			
			it = CreateItem("Document", "paper", r\x - 1440.0 * RoomScale, r\y + 624.0 * RoomScale, r\z + 152.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)	
			
			it = CreateItem("Radio Transceiver", "radio", r\x - 1184.0 * RoomScale, r\y + 480.0 * RoomScale, r\z - 800.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)				
			
			For i = 0 To Rand(0, 1)
				it = CreateItem("ReVision Eyedrops", "eyedrops", r\x - 1529.0 * RoomScale, r\y + 563.0 * RoomScale, r\z - 572.0 * RoomScale + i * 0.05)
				EntityParent(it\Collider, r\OBJ)				
			Next
			
			it = CreateItem("9V Battery", "bat", r\x - 1545.0 * RoomScale, r\y + 603.0 * RoomScale, r\z - 372.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			If Rand(2) = 1 Then
				it = CreateItem("9V Battery", "bat", r\x - 1540.0 * RoomScale, r\y + 603.0 * RoomScale, r\z - 340.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)
			EndIf
			
			If Rand(2) = 1 Then
				it = CreateItem("9V Battery", "bat", r\x - 1529.0 * RoomScale, r\y + 603.0 * RoomScale, r\z - 308.0 * RoomScale)
				EntityParent(it\Collider, r\OBJ)
			EndIf
			;[End Block]
		Case "start"
			;[Block]
			; ~ The containment doors
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 4000.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 1696.0 * RoomScale, 90.0, r, True, True)
			r\RoomDoors[1]\Locked = False : r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\MTFClose = False : r\RoomDoors[1]\Open = True
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[1]\Buttons[i]) : r\RoomDoors[1]\Buttons[i] = 0
			Next
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x + 2704.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 624.0 * RoomScale, 90.0, r)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = False : r\RoomDoors[2]\MTFClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[2]\Buttons[i]) : r\RoomDoors[2]\Buttons[i] = 0
			Next
			
			d = CreateDoor(r\Zone, r\x + 1392.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 64.0 * RoomScale, 90.0, r, True)
			d\AutoClose = False : d\Locked = True : d\MTFClose = False
			
			d = CreateDoor(r\Zone, r\x - 640.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 64.0 * RoomScale, 90.0, r)
			d\Locked = True : d\AutoClose = False
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			d = CreateDoor(r\Zone, r\x + 1280.0 * RoomScale, r\y + 383.9 * RoomScale, r\z + 312.0 * RoomScale, 180.0, r, True)
			d\Locked = True : d\AutoClose = False : d\MTFClose = False
			PositionEntity(d\Buttons[0], r\x + 1120.0 * RoomScale, EntityY(d\Buttons[0], True), r\z + 322.0 * RoomScale, True)
			PositionEntity(d\Buttons[1], r\x + 1120.0 * RoomScale, EntityY(d\Buttons[1], True), r\z + 302.0 * RoomScale, True)
			FreeEntity(d\OBJ2) : d\OBJ2 = 0
			
			d = CreateDoor(r\Zone, r\x, r\y, r\z + 1184.0 * RoomScale, 0.0, r)
			d\Locked = True : d\MTFClose = False
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			r\Objects[0] = LoadMesh_Strict("GFX\map\IntroDesk.b3d")
			ScaleEntity(r\Objects[0], RoomScale, RoomScale ,RoomScale)
			PositionEntity(r\Objects[0], r\x + 272.0 * RoomScale, r\y, r\z + 400.0 * RoomScale)
			
			r\Objects[1] = LoadMesh_Strict("GFX\map\IntroDrawer.b3d")
			ScaleEntity(r\Objects[1], RoomScale, RoomScale ,RoomScale)
			PositionEntity(r\Objects[1], r\x + 448.0 * RoomScale, r\y, r\z + 192.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], EntityX(r\OBJ) + 40.0 * RoomScale, r\y + 460.0 * RoomScale, EntityZ(r\OBJ) + 1072.0 * RoomScale)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], EntityX(r\OBJ) - 80.0 * RoomScale, r\y + 100.0 * RoomScale, EntityZ(r\OBJ) + 526.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], EntityX(r\OBJ) - 128.0 * RoomScale, r\y + 100.0 * RoomScale, EntityZ(r\OBJ) + 320.0 * RoomScale)
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], EntityX(r\OBJ) + 660.0 * RoomScale, r\y + 100.0 * RoomScale, EntityZ(r\OBJ) + 526.0 * RoomScale)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], EntityX(r\OBJ) + 700 * RoomScale, r\y + 100.0 * RoomScale, EntityZ(r\OBJ) + 320.0 * RoomScale)
			
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], EntityX(r\OBJ) + 1472.0 * RoomScale, r\y + 100.0 * RoomScale, EntityZ(r\OBJ) + 912.0 * RoomScale)
			
			For i = 0 To 7
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			sc = CreateSecurityCam(r\x - 336.0 * RoomScale, r\y + 352.0 * RoomScale, r\z + 48.0 * RoomScale, r, True)
			sc\Angle = 270 : sc\Turn = 45 : sc\room = r
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			PositionEntity(sc\ScrOBJ, r\x + 1456.0 * RoomScale, r\y + 608.0 * RoomScale, r\z + 352.0 * RoomScale)
			TurnEntity(sc\ScrOBJ, 0.0, 90.0, 0.0)
			EntityParent(sc\ScrOBJ, r\OBJ)
			
			; ~ White smoke under balcony
			CreateDevilEmitter(r\x + 3384.0 * RoomScale, r\y + 510.0 * RoomScale, r\z + 2400.0 * RoomScale, r, 1, 4)
			
			de = CreateDecal(0, r\x + 272.0 * RoomScale, r\y + 0.005, r\z + 262.0 * RoomScale, 90.0, Rand(360.0), 0.0)
			EntityParent(de\OBJ, r\OBJ)
			
			de = CreateDecal(0, r\x + 456.0 * RoomScale, r\y + 0.005, r\z + 135.0 * RoomScale, 90.0, Rand(360.0), 0.0)
			EntityParent(de\OBJ, r\OBJ)
			
			For i = 0 To 4
			    Select i
			        Case 0
						;[Block]
			            dX = 5222.0
			            dZ = 1224.0  
			            dSize = 0.54
			            dID = 4
						;[End Block]
			        Case 1
						;[Block]
			            dX = 5190.0
			            dZ = 2270.0
			            dSize = 1.2
			            dID = 4
						;[End Block]
			        Case 2
						;[Block]
			            dX = 4305
			            dZ = 1234.0
			            dSize = 0.44
			            dID = 4
						;[End Block]
			        Case 3
						;[Block]
			            dX = 4320.0 
			            dZ = 2000.0
			            dSize = 0.54
			            dID = 4
						;[End Block]
			        Case 4
						;[Block]
			            dX = 4978.0
			            dZ = 1985.0
			            dSize = 0.54
			            dID = 6
						;[End Block]
			    End Select
			    de = CreateDecal(dID, r\x + dX * RoomScale, r\y + 386.0 * RoomScale, r\z + dZ * RoomScale, 90.0, 45.0, 0.0)
			    de\Size = dSize
			    de\Alpha = Rnd(0.8, 1.0)
			    ScaleSprite(de\OBJ, de\Size, de\Size)
			Next
			;[End Block]
		Case "room2scps"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 272.0 * RoomScale, r\y, r\z, 90.0, r, True, False, 3)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) + 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) - 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			d = CreateDoor(r\Zone, r\x - 272.0 * RoomScale, r\y, r\z, 270.0, r, True, False, 3)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.061, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			d = CreateDoor(r\Zone, r\x - 560.0 * RoomScale, r\y, r\z - 272.0 * RoomScale, 0.0, r, True, False, 3)
			d\AutoClose = False : d\Open = False
			
			d = CreateDoor(r\Zone, r\x + 560.0 * RoomScale, r\y, r\z - 272.0 * RoomScale, 180.0, r, True, False, 3)
			d\AutoClose = False : d\Open = False
			
			d = CreateDoor(r\Zone, r\x + 560.0 * RoomScale, r\y, r\z + 272.0 * RoomScale, 180.0, r, True, False, 3)
			d\AutoClose = False : d\Open = False
			
			d = CreateDoor(r\Zone, r\x - 560.0 * RoomScale, r\y, r\z + 272.0 * RoomScale, 0.0, r, True, False, 3)
            d\AutoClose = False : d\Open = False
			
			For i = 0 To 3
				Select i
					Case 0
						;[Block]
						scX = 560.0
						scZ = -416.0
						scAngle = 180.0
						;[End Block]
					Case 1
						;[Block]
						scX = -560.0
						scZ = -416.0
						scAngle = 180.0
						;[End Block]
					Case 2
						;[Block]
						scX = 560.0
						scZ = 480.0
						scAngle = 0.0
						;[End Block]
					Case 3
						;[Block]
						scX = -560.0
						scZ = 480.0
						scAngle = 0.0
						;[End Block]
				End Select
				sc = CreateSecurityCam(r\x + scX * RoomScale, r\y + 386.0 * RoomScale, r\z + scZ * RoomScale, r)
				sc\Angle = scAngle : sc\Turn = 30.0
				TurnEntity(sc\CameraOBJ, 30.0, 0.0, 0.0)
				EntityParent(sc\OBJ, r\OBJ)
			Next
			
			For i = 0 To 14
				Select i
					Case 0
						;[Block]
						dX = -64.0
						dZ = -516.0
						;[End Block]
					Case 1
						;[Block]
						dX = -96.0
						dZ = -388.0
						;[End Block]
					Case 2
						;[Block]
						dX = -128.0
						dZ = -292.0
						;[End Block]
					Case 3
						;[Block]
						dX = -128.0
						dZ = -132.0
						;[End Block]
					Case 4
						;[Block]
						dX = -160.0
						dZ = -36.0
						;[End Block]
					Case 5
						;[Block]
						dX = -192.0
						dZ = 28.0
						;[End Block]
					Case 6
						;[Block]
						dX = -384.0
						dZ = 28.0
						;[End Block]
					Case 7
						;[Block]
						dX = -448.0
						dZ = 92.0
						;[End Block]
					Case 8
						;[Block]
						dX = -480.0
						dZ = 124.0
						;[End Block]
					Case 9
						;[Block]
						dX = -512.0
						dZ = 156.0
						;[End Block]
					Case 10
						;[Block]
						dX = -544.0
						dZ = 220.0
						;[End Block]
					Case 11
						;[Block]
						dX = -544.0
						dZ = 380.0
						;[End Block]
					Case 12
						;[Block]
						dX = -544.0
						dZ = 476.0
						;[End Block]
					Case 13
						;[Block]
						dX = -544.0
						dZ = 572.0
						;[End Block]
					Case 14
						;[Block]
						dX = -544.0
						dZ = 636.0
						;[End Block]
				End Select
				de = CreateDecal(Rand(15, 16), r\x + dX * RoomScale, r\y + 0.005, r\z + dZ * RoomScale, 90.0, Rand(360.0), 0.0)
				If i > 10 Then
					de\Size = Rnd(0.2, 0.25)
				Else
					de\Size = Rnd(0.1, 0.17)
				EndIf
				EntityAlpha(de\OBJ, 1.0) : ScaleSprite(de\OBJ, de\Size, de\Size)
				EntityParent(de\OBJ, r\OBJ)
			Next
			
			it = CreateItem("SCP-714", "scp714", r\x - 552.0 * RoomScale, r\y + 220.0 * RoomScale, r\z - 760.0 * RoomScale)
			EntityParent(it\collider, r\OBJ)
			
			it = CreateItem("SCP-1025", "scp1025", r\x + 552.0 * RoomScale, r\y + 224.0 * RoomScale, r\z - 758.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("SCP-860", "scp860", r\x + 568.0 * RoomScale, r\y + 178.0 * RoomScale, r\z + 750.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-714", "paper", r\x - 728.0 * RoomScale, r\y + 288.0 * RoomScale, r\z - 360.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-427", "paper", r\x - 608.0 * RoomScale, r\y + 66.0 * RoomScale, r\z + 636.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room205"
			;[Block]
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 128.0 * RoomScale, r\y, r\z + 640.0 *RoomScale, 90.0, r, True, False, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False
			
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x - 1392.0 * RoomScale, r\y - 128.0 * RoomScale, r\z - 384.0 * RoomScale, 0.0, r, True, False, 3, "", True)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[0]\Buttons[i]) : r\RoomDoors[0]\Buttons[i] = 0
			Next
			
			sc = CreateSecurityCam(r\x - 1152.0 * RoomScale, r\y + 900.0 * RoomScale, r\z + 176.0 * RoomScale, r, True)
			sc\Angle = 90.0 : sc\Turn = 0.0
			EntityParent(sc\OBJ, r\OBJ)
			sc\AllowSaving = False
			sc\RenderInterval = 0
			PositionEntity(sc\ScrOBJ, r\x - 1716.0 * RoomScale, r\y + 160.0 * RoomScale, r\z + 176.0 * RoomScale, True)
			TurnEntity(sc\ScrOBJ, 0.0, 90.0, 0.0)
			ScaleSprite(sc\ScrOBJ, 896.0 * 0.5 * RoomScale, 896.0 * 0.5 * RoomScale)
			EntityParent(sc\ScrOBJ, r\OBJ)
			CameraZoom (sc\Cam, 1.5)
			HideEntity(sc\ScrOverlay)
			HideEntity(sc\MonitorOBJ)
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 1536.0 * RoomScale, r\y + 730.0 * RoomScale, r\z + 192.0 * RoomScale)
			RotateEntity(r\Objects[0], 0.0, -90.0, 0.0)
			EntityParent(r\Objects[0], r\OBJ)
			
			r\Objects[1] = sc\ScrOBJ
			;[End Block]
		Case "endroom"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x, r\y, r\z + 1136.0 * RoomScale, r\y, r, False, True)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[0]\Buttons[i]) : r\RoomDoors[0]\Buttons[i] = 0
			Next
			;[End Block]
		Case "room895"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x, r\y, r\z - 448.0 * RoomScale, 0.0, r, False, True, 2)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = False
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x - 390.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[i], Trye), r\z - 280.0 * RoomScale, True)
            PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\buttons[1], True) + 0.025, EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True), True) 
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x, - 1320.0 * RoomScale, r\z + 2304.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 96.0 * RoomScale, r\y - 1532.0 * RoomScale, r\z + 2016.0 * RoomScale)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[2] = CopyEntity(o\LeverModelID[0])
			r\Objects[3] = CopyEntity(o\LeverModelID[1])
			
			r\Levers[0] = r\Objects[3]
			
			For i = 0 To 1
				ScaleEntity(r\Objects[2 + i], 0.04, 0.04, 0.04)
				PositionEntity(r\Objects[2 + i], r\x - 800.0 * RoomScale, r\y + 180.0 * RoomScale, r\z - 336.0 * RoomScale)
				EntityParent(r\Objects[2 + i], r\OBJ)
			Next
			RotateEntity(r\Objects[2], 0.0, 180.0, 0.0)
			RotateEntity(r\Objects[3], 10.0, 0.0, 0.0)
			EntityPickMode(r\Objects[3], 1, False)
			EntityRadius(r\Objects[3], 0.1)
			
			sc = CreateSecurityCam(r\x - 320.0 * RoomScale, r\y + 704.0 * RoomScale, r\z + 288.0 * RoomScale, r, True)
			sc\Angle = 45 + 180
			sc\Turn = 45
			sc\CoffinEffect = True
			TurnEntity(sc\CameraOBJ, 120.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			CoffinCam = sc
			PositionEntity(sc\ScrOBJ, r\x - 800.0 * RoomScale, r\y + 288.0 * RoomScale, r\z - 340.0 * RoomScale)
			EntityParent(sc\ScrOBJ, r\OBJ)
			TurnEntity(sc\ScrOBJ, 0.0, 180.0, 0.0)
			
			it = CreateItem("Document SCP-895", "paper", r\x - 688.0 * RoomScale, r\y + 133.0 * RoomScale, r\z - 304.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Level 3 Key Card", "key3", r\x + 240.0 * RoomScale, r\y - 1456.0 * RoomScale, r\z + 2064.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Night Vision Goggles", "nvgoggles", r\x + 280.0 * RoomScale, r\y - 1456.0 * RoomScale, r\z + 2164.0 * RoomScale)
			it\State = 400.0
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2tesla", "room2tesla_lcz", "room2tesla_hcz"
			;[Block]
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 114.0 * RoomScale, r\y, r\z)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x + 114.0 * RoomScale, r\y, r\z)		
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x, r\y, r\z)	
			
			r\Objects[3] = CreateSprite()
			EntityTexture(r\Objects[3], TeslaTexture)
			SpriteViewMode(r\Objects[3], 2) 
			EntityBlend(r\Objects[3], 3) 
			EntityFX(r\Objects[3], 1 + 8 + 16)
			PositionEntity(r\Objects[3], r\x, r\y + 0.8, r\z)
			HideEntity(r\Objects[3])
			
			r\Objects[4] = CreateSprite()
			PositionEntity(r\Objects[4], r\x - 32.0 * RoomScale, r\y + 568.0 * RoomScale, r\z)
			ScaleSprite(r\Objects[4], 0.03, 0.03)
			EntityTexture(r\Objects[4], LightSpriteTex(1))
			EntityBlend(r\Objects[4], 3)
			HideEntity(r\Objects[4])
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x, r\y, r\z-800.0 * RoomScale)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x, r\y, r\z + 800.0 * RoomScale)
			
			For r2.Rooms = Each Rooms
				If r2 <> r Then
					If r2\RoomTemplate\Name = "room2tesla" Or r2\RoomTemplate\Name = "room2tesla_lcz" Or r2\RoomTemplate\Name = "room2tesla_hcz" Then
						r\Objects[7] = CopyEntity(r2\Objects[7], r\OBJ) ; ~ Don't load the mesh again
						Exit
					EndIf
				EndIf
			Next
			If r\Objects[7] = 0 Then r\Objects[7] = LoadMesh_Strict("GFX\map\room2tesla_caution.b3d", r\OBJ)
			
			For i = 0 To 6
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			w.WayPoints = CreateWaypoint(r\x, r\y + 66.0 * RoomScale, r\z + 292.0 * RoomScale, Null, r)
			w2.WayPoints = CreateWaypoint(r\x, r\y + 66.0 * RoomScale, r\z - 284.0 * RoomScale, Null, r)
			w\Connected[0] = w2 : w\Dist[0] = EntityDistance(w\OBJ, w2\OBJ)
			w2\Connected[0] = w : w2\Dist[0] = w\Dist[0]
			;[End Block]
		Case "room2doors"
			;[Block]
			d = CreateDoor(r\Zone, r\x, r\y, r\z + 528.0 * RoomScale, 0.0, r, True)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], r\x - 832.0 * RoomScale, EntityY(d\Buttons[0], True), r\z + 167.0 * RoomScale, True) 
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.061, True)
			
			d2 = CreateDoor(r\Zone, r\x, r\y, r\z - 528.0 * RoomScale, 180.0, r, True)
			d2\AutoClose = False : d2\Open = True
			FreeEntity(d2\Buttons[0]) : d2\Buttons[0] = 0
			PositionEntity(d2\Buttons[1], EntityX(d2\Buttons[1], True), EntityY(d2\Buttons[1], True), EntityZ(d2\Buttons[1], True) + 0.061, True)
			
			d\LinkedDoor = d2
			d2\LinkedDoor = d
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x - 832.0 * RoomScale, r\y + 0.5, r\z)
			EntityParent(r\Objects[0], r\OBJ)
			;[End Block]
		Case "room914"
			;[Block]
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x, r\y, r\z - 368.0 * RoomScale, 0.0, r, False, True, 2)
			r\RoomDoors[2]\AutoClose = False
			PositionEntity(r\RoomDoors[2]\Buttons[0], r\x - 496.0 * RoomScale, EntityY(r\RoomDoors[2]\Buttons[0], True), r\z - 278.0 * RoomScale, True)
			PositionEntity(r\RoomDoors[2]\Buttons[1], EntityX(r\RoomDoors[2]\Buttons[1], True) + 0.025, EntityY(r\RoomDoors[2]\Buttons[1], True), EntityZ(r\RoomDoors[2]\Buttons[1], True), True) 
			TurnEntity(r\RoomDoors[2]\Buttons[0], 0.0, 90.0, 0.0)
			
			d = CreateDoor(r\Zone, r\x - 1036.0 * RoomScale, r\y, r\z + 528.0 * RoomScale, 180.0, r, True, 5)
			FreeEntity(d\OBJ2) : d\OBJ2 = 0
			
			For i = 0 To 1
				FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
			Next
			
			r\RoomDoors[0] = d : d\AutoClose = False
			
			d = CreateDoor(r\Zone, r\x + 404.0 * RoomScale, r\y, r\z + 528.0 * RoomScale, 180.0, r, True, 5)
			FreeEntity(d\OBJ2) : d\OBJ2 = 0	
			
			For i = 0 To 1
				FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
			Next
			
			r\RoomDoors[1] = d : d\AutoClose = False
			
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x - 448.0 * RoomScale, r\y, r\z - 705.0 * RoomScale, 90.0, r, False, False, 2)
			r\RoomDoors[3]\AutoClose = False
			PositionEntity(r\RoomDoors[3]\Buttons[0], EntityX(r\RoomDoors[3]\Buttons[0], True) - 0.061, EntityY(r\RoomDoors[3]\Buttons[0], True), EntityZ(r\RoomDoors[3]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[3]\Buttons[1], EntityX(r\RoomDoors[3]\Buttons[1], True) + 0.061, EntityY(r\RoomDoors[3]\Buttons[1], True), EntityZ(r\RoomDoors[3]\Buttons[1], True), True)
			
			r\Objects[0] = LoadMesh_Strict("GFX\map\914key.x")
			PositionEntity(r\Objects[0], r\x - 416.0 * RoomScale, r\y + 190.0 * RoomScale, r\z + 374.0 * RoomScale, True)
			
			r\Objects[1] = LoadMesh_Strict("GFX\map\914knob.x")
			PositionEntity(r\Objects[1], r\x - 416.0 * RoomScale, r\y + 230.0 * RoomScale, r\z + 374.0 * RoomScale, True)
			
			For i = 0 To 1
				ScaleEntity(r\Objects[i], RoomScale, RoomScale, RoomScale, True)
				EntityPickMode(r\Objects[i], 2)
			Next
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], r\x - 1132.0 * RoomScale, r\y + 0.5, r\z + 640.0 * RoomScale)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x + 308.0 * RoomScale, r\y + 0.5, r\z + 640.0 * RoomScale)
			
			For i = 0 To 3
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Addendum: 5/14 Test Log", "paper", r\x + 538.0 * RoomScale, r\y + 228.0 * RoomScale, r\z + 127.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			RotateEntity(it\Collider, 0.0, 0.0, 0.0)	
			
			it = CreateItem("First Aid Kit", "firstaid", r\x + 538.0 * RoomScale, r\y + 112.0 * RoomScale, r\z - 40.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			RotateEntity(it\Collider, 0.0, 90.0, 0.0)
			
			it = CreateItem("Dr. L's Note", "paper", r\x - 538.0 * RoomScale, r\y + 250.0 * RoomScale, r\z - 365.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
		Case "173"
			;[Block]
			r\RoomDoors[1] = CreateDoor(r\Zone, EntityX(r\OBJ) + 288.0 * RoomScale, r\y, EntityZ(r\OBJ) + 384.0 * RoomScale, 90.0, r, False, True)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = False : r\RoomDoors[1]\MTFClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[1]\Buttons[i]) : r\RoomDoors[1]\Buttons[i] = 0
			Next
			
			r\RoomDoors[2] = CreateDoor(r\Zone, r\x - 1008.0 * RoomScale, r\y, r\z - 688.0 * RoomScale, 90.0, r, True, False, False, "", True)
			r\RoomDoors[2]\AutoClose = False : r\RoomDoors[2]\Open = False : r\RoomDoors[2]\Locked = True : r\RoomDoors[2]\MTFClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[2]\Buttons[i]) : r\RoomDoors[2]\Buttons[i] = 0
			Next
			
			r\RoomDoors[3] = CreateDoor(r\Zone, r\x - 2324.0 * RoomScale, r\y, r\z - 1248.0 * RoomScale, 90.0, r)
			r\RoomDoors[3]\AutoClose = False : r\RoomDoors[3]\Open = True : r\RoomDoors[3]\Locked = True : r\RoomDoors[3]\MTFClose = False
			PositionEntity(r\RoomDoors[3]\Buttons[0], EntityX(r\RoomDoors[3]\Buttons[0], True) - 4.0 * RoomScale, EntityY(r\RoomDoors[3]\Buttons[0], True), EntityZ(r\RoomDoors[3]\Buttons[0], True), True)
			PositionEntity(r\RoomDoors[3]\Buttons[1], EntityX(r\RoomDoors[3]\Buttons[1], True) + 4.0 * RoomScale, EntityY(r\RoomDoors[3]\Buttons[1], True), EntityZ(r\RoomDoors[3]\Buttons[1], True), True)
			
			r\RoomDoors[4] = CreateDoor(r\Zone, r\x - 4352.0 * RoomScale, r\y, r\z - 1248.0 * RoomScale, 90.0, r)
			r\RoomDoors[4]\AutoClose = False : r\RoomDoors[4]\Open = True : r\RoomDoors[4]\Locked = True : r\RoomDoors[4]\MTFClose = False	
			
			; ~ The door in the office below the walkway
			r\RoomDoors[7] = CreateDoor(r\Zone, r\x - 3712.0 * RoomScale, r\y - 385.0 * RoomScale, r\z - 128.0 * RoomScale, 0.0, r)
			r\RoomDoors[7]\AutoClose = False : r\RoomDoors[7]\Open = True : r\RoomDoors[7]\MTFClose = False : r\RoomDoors[7]\Locked = True
			FreeEntity(r\RoomDoors[7]\Buttons[1]) : r\RoomDoors[7]\Buttons[1] = 0
			
			d = CreateDoor(r\Zone, r\x - 3712 * RoomScale, r\y - 385.0 * RoomScale, r\z - 2336.0 * RoomScale, 0.0, r)
			d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
			FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
			
			; ~ The door from the concrete tunnel to the large hall
			d = CreateDoor(r\Zone, r\x - 6864.0 * RoomScale, r\y, r\z - 1248.0 * RoomScale, 90.0, r, True)
			d\AutoClose = False : d\Locked = True : d\MTFClose = False
			
			; ~ The locked door to the lower level of the hall
			d = CreateDoor(r\Zone, r\x - 5856.0 * RoomScale, r\y, r\z - 1504.0 * RoomScale, 0.0, r)
			d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
			
			; ~ The door to the staircase in the office room
			d = CreateDoor(r\Zone, r\x - 2432.0 * RoomScale, r\y, r\z - 1000.0 * RoomScale, 0.0, r)
			d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
			PositionEntity(d\Buttons[0], r\x - 2592.0 * RoomScale, EntityY(d\Buttons[0], True), r\z - 1010.0 * RoomScale, True)
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			Tex = LoadTexture_Strict("GFX\map\Door02.jpg")
			For zTemp = 0 To 1
				d = CreateDoor(r\Zone, r\x - 5760.0 * RoomScale, r\y, r\z + (320.0 + 896.0 * zTemp) * RoomScale, 0.0, r)
				d\Locked = True : d\DisableWaypoint = True
				
				If zTemp = 0 Then
				    FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
				Else
				    FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
				EndIf
				
				d = CreateDoor(r\Zone, r\x - 8288.0 * RoomScale, r\y, r\z + (320.0 + 896.0 * zTemp) * RoomScale, 0.0, r)
				d\Locked = True : d\MTFClose = False
				If zTemp = 0 Then 
				    d\Open = True
				Else 
				    d\DisableWaypoint = True
				    FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
				EndIf
				
				For xTemp = 0 To 2
					d = CreateDoor(r\Zone, r\x - (7424.0 - 512.0 * xTemp) * RoomScale, r\y, r\z + (1008.0 - 480.0 * zTemp) * RoomScale, 180.0 * (Not zTemp), r)
					d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
					EntityTexture(d\OBJ, Tex)
					FreeEntity(d\OBJ2) : d\OBJ2 = 0
					
					For i = 0 To 1
						FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
					Next
				Next					
				For xTemp = 0 To 4
					d = CreateDoor(r\Zone, r\x - (5120.0 - 512.0 * xTemp) * RoomScale, r\y, r\z + (1008.0 - 480.0 * zTemp) * RoomScale, 180.0 * (Not zTemp), r)
					d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
					EntityTexture(d\OBJ, Tex)
					FreeEntity(d\OBJ2) : d\OBJ2 = 0
					
					For i = 0 To 1
						FreeEntity(d\Buttons[i]) : d\Buttons[i] = 0
					Next
					
					If xTemp = 2 And zTemp = 1 Then r\RoomDoors[6] = d
				Next	
			Next
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], EntityX(r\OBJ) + 40.0 * RoomScale, 460.0 * RoomScale, EntityZ(r\OBJ) + 1072.0 * RoomScale)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], EntityX(r\OBJ) - 80.0 * RoomScale, 100.0 * RoomScale, EntityZ(r\OBJ) + 480.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], EntityX(r\OBJ) - 128.0 * RoomScale, 100.0 * RoomScale, EntityZ(r\OBJ) + 320.0 * RoomScale)
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], EntityX(r\OBJ) + 660.0 * RoomScale, 100.0 * RoomScale, EntityZ(r\OBJ) + 526.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], EntityX(r\OBJ) + 700 * RoomScale, 100.0 * RoomScale, EntityZ(r\OBJ) + 320.0 * RoomScale)
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], EntityX(r\OBJ) + 1472.0 * RoomScale, 100.0 * RoomScale, EntityZ(r\OBJ) + 912.0 * RoomScale)
			
			For i = 0 To 5
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[9] = LoadMesh_Strict("GFX\map\173_2.b3d", r\OBJ)
			EntityType(r\Objects[9], HIT_MAP)
			EntityPickMode(r\Objects[9], 2)
			
			r\Objects[10] = LoadMesh_Strict("GFX\map\intro_labels.b3d", r\OBJ)
			PositionEntity(r\Objects[10], EntityX(r\Objects[10], True), EntityY(r\Objects[10], True) - 10.0 * RoomScale, EntityZ(r\Objects[10], True), True)
			
			For i = 0 To 4
			    Select i
			        Case 0
						;[Block]
			            dX = 1472.0
			            dZ = 912.0  
			            dSize = 1.2
			            dID = 4
						;[End Block]
			        Case 1
						;[Block]
			            dX = 1504.0
			            dZ = -80.0
			            dSize = 0.54
			            dID = 4
						;[End Block]
			        Case 2
						;[Block]
			            dX = 587.0
			            dZ = -70.0
			            dSize = 0.44
			            dID = 4
						;[End Block]
			        Case 3
						;[Block]
			            dX = 602.0 
			            dZ = 642.0
			            dSize = 0.54
			            dID = 4
						;[End Block]
			        Case 4
						;[Block]
			            dX = 1260.0
			            dZ = 627.0
			            dSize = 0.54
			            dID = 6
						;[End Block]
			    End Select
			    de = CreateDecal(dID, r\x + dX * RoomScale, r\y + 2.0 * RoomScale, r\z + dZ * RoomScale, 90.0, 45.0, 0.0)
			    de\Size = dSize : de\Alpha = Rnd(0.8, 1.0)
			    ScaleSprite(de\OBJ, de\Size, de\Size)
			Next
			
			sc = CreateSecurityCam(r\x - 4048.0 * RoomScale, r\y - 32.0 * RoomScale, r\z - 1232.0 * RoomScale, r, True)
			sc\Angle = 270.0 : sc\Turn = 45.0 : sc\room = r
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			PositionEntity(sc\ScrOBJ, r\x - 2256.0 * RoomScale, r\y + 224.0 * RoomScale, r\z - 928.0 * RoomScale)
			TurnEntity(sc\ScrOBJ, 0.0, 90.0, 0.0)
			EntityParent(sc\ScrOBJ, r\OBJ)
			
			it = CreateItem("Class D Orientation Leaflet", "paper", r\x - (2914.0 + 1024.0) * RoomScale, r\y + 170.0 * RoomScale, r\z + 40.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2ccont"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 64.0 * RoomScale, r\y, r\z + 368.0 * RoomScale, 180.0, r, False, False, 3)
			d\AutoClose = False : d\Open = False
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0],True) + 0.061, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1],True) - 0.061, True)
			
			For k = 0 To 2
				r\Objects[k * 2] = CopyEntity(o\LeverModelID[0])
				r\Objects[k * 2 + 1] = CopyEntity(o\LeverModelID[1])
				
				r\Levers[k] = r\Objects[k * 2 + 1]
				
				For i = 0 To 1
					ScaleEntity(r\Objects[k * 2 + i], 0.04, 0.04, 0.04)
					PositionEntity(r\Objects[k * 2 + i], r\x - 240.0 * RoomScale, r\y + 1104.0 * RoomScale, r\z + (632.0 - 64.0 * k) * RoomScale)
					EntityParent(r\Objects[k * 2 + i], r\OBJ)
				Next
				RotateEntity(r\Objects[k * 2], 0.0, -90.0, 0.0)
				RotateEntity(r\Objects[k * 2 + 1], 10.0, -90.0 - 180.0, 0.0)
				EntityPickMode(r\Objects[k * 2 + 1], 1, False)
				EntityRadius(r\Objects[k * 2 + 1], 0.1)
			Next
			
			sc = CreateSecurityCam(r\x - 265.0 * RoomScale, r\y + 1280.0 * RoomScale, r\z + 105.0 * RoomScale, r)
			sc\Angle = 45.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			
			it = CreateItem("Note from Daniel", "paper", r\x - 400.0 * RoomScale, r\y + 1040.0 * RoomScale, r\z + 115.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room106"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 968.0 * RoomScale, r\y - 764.0 * RoomScale, r\z + 1392.0 * RoomScale, 0.0, r, False, False, 4)
			d\AutoClose = False : d\Open = False	
			
			d = CreateDoor(r\Zone, r\x, r\y, r\z - 464.0 * RoomScale, 0.0, r, False, False, 4)
			d\AutoClose = False : d\Open = False			
			
			d = CreateDoor(r\Zone, r\x - 624.0 * RoomScale, r\y - 1280.0 * RoomScale, r\z, 90.0, r, False, False, 4)
			d\AutoClose = False : d\Open = False	
			
			r\Objects[4] = CreateButton(r\x - 146.0 * RoomScale, r\y - 576.0 * RoomScale, r\z + 3045.0 * RoomScale, 0.0, 0.0, 0.0)
			
			r\Objects[5] = CreatePivot()
			TurnEntity(r\Objects[5], 0.0, 180.0, 0.0)
			PositionEntity(r\Objects[5], r\x + 1088.0 * RoomScale, r\y + 1104.0 * RoomScale, r\z + 1888.0 * RoomScale) 
			
			r\Objects[6] = LoadMesh_Strict("GFX\map\room1062.b3d")
			ScaleEntity(r\Objects[6], RoomScale, RoomScale, RoomScale)
			EntityType(r\Objects[6], HIT_MAP)
			EntityPickMode(r\Objects[6], 3)
			PositionEntity(r\Objects[6], r\x + 784.0 * RoomScale, r\y - 980.0 * RoomScale, r\z + 720.0 * RoomScale)
			
			For i = 4 To 6
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For k = 0 To 2 Step 2
				r\Objects[k] = CopyEntity(o\LeverModelID[0])
				r\Objects[k + 1] = CopyEntity(o\LeverModelID[1])
				
				r\Levers[k / 2] = r\Objects[k + 1]
				
				For i = 0 To 1
					ScaleEntity(r\Objects[k + i], 0.04, 0.04, 0.04)
					PositionEntity(r\Objects[k + i], r\x - (555.0 - 81.0 * (k / 2.0)) * RoomScale, r\y - 576.0 * RoomScale, r\z + 3040.0 * RoomScale)
					EntityParent(r\Objects[k + i], r\OBJ)
				Next
				RotateEntity(r\Objects[k], 0.0, 0.0, 0.0)
				RotateEntity(r\Objects[k + 1], 10.0, -180.0, 0.0)
				EntityPickMode(r\Objects[k + 1], 1, False)
				EntityRadius(r\Objects[k + 1], 0.1)
			Next
			RotateEntity(r\Objects[1], 81.0, -180.0, 0.0)
			RotateEntity(r\Objects[3], -81.0, -180.0, 0.0)			
			
			sc = CreateSecurityCam(r\x + 768.0 * RoomScale, r\y + 1392.0 * RoomScale, r\z + 1696.0 * RoomScale, r, True)
			sc\Angle = 45.0 + 90.0 + 180.0 : sc\Turn = 20.0
			TurnEntity(sc\CameraOBJ, 45.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			r\Objects[7] = sc\CameraOBJ
			r\Objects[8] = sc\OBJ
			PositionEntity(sc\ScrOBJ, r\x - 272.0 * RoomScale, r\y - 544.0 * RoomScale, r\z + 3020.0 * RoomScale)
			TurnEntity(sc\ScrOBJ, 0.0, -10.0, 0.0)
			EntityParent(sc\ScrOBJ, r\OBJ)
			sc\CoffinEffect = 0
			
			r\Objects[9] = CreatePivot()
			PositionEntity(r\Objects[9], r\x - 272.0 * RoomScale, r\y - 672.0 * RoomScale, r\z + 2736.0 * RoomScale)
			
			r\Objects[10] = CreatePivot()
			PositionEntity(r\Objects[10], r\x, r\y, r\z - 720.0 * RoomScale)
			
			For i = 9 To 10
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("Level 5 Key Card", "key5", r\x - 752.0 * RoomScale, r\y - 592.0 * RoomScale, r\z + 3026.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Dr. Allok's Note", "paper", r\x - 416.0 * RoomScale, r\y - 576.0 * RoomScale, r\z + 2492.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Recall Protocol RP-106-N", "paper", r\x + 268.0 * RoomScale, r\y - 576.0 * RoomScale, r\z + 2593.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room1archive"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x, r\y, r\z - 528.0 * RoomScale, 0.0, r, False, False, Rand(1, 3))
			PositionEntity(r\RoomDoors[0]\Buttons[0], EntityX(r\RoomDoors[0]\Buttons[0], True), EntityY(r\RoomDoors[0]\Buttons[0], True), EntityZ(r\RoomDoors[0]\Buttons[0], True) + 0.061, True)
			PositionEntity(r\RoomDoors[0]\Buttons[1], EntityX(r\RoomDoors[0]\Buttons[1], True), EntityY(r\RoomDoors[0]\Buttons[1], True), EntityZ(r\RoomDoors[0]\Buttons[1], True) - 0.061, True)
			
			sc = CreateSecurityCam(r\x - 256.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 640.0 * RoomScale, r)
			sc\Angle = 180.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			
			For xTemp = 0 To 1
				For yTemp = 0 To 2
					For zTemp = 0 To 2
						TempStr$ = "9V Battery" : TempStr2$ = "bat"
						Chance% = Rand(-10, 100)
						Select True
							Case (Chance < 0)
								;[Block]
								Exit
								;[End Block]
							Case (Chance < 40) ; ~ 40% chance for a document
								;[Block]
								TempStr = "Document SCP-"
								Select Rand(19)
									Case 1
										;[Block]
										TempStr = TempStr + "008"
										;[End Block]
									Case 2
										;[Block]
										TempStr = TempStr + "012"
										;[End Block]
									Case 3
										;[Block]
										TempStr = TempStr + "035"
										;[End Block]
									Case 4
										;[Block]
										TempStr = TempStr + "049"
										;[End Block]
									Case 5
										;[Block]
										TempStr = TempStr + "096"
										;[End Block]
									Case 6
										;[Block]
										TempStr = TempStr + "106"
										;[End Block]
									Case 7
										;[Block]
										TempStr = TempStr + "173"
										;[End Block]
									Case 8
										;[Block]
										TempStr = TempStr + "513"
										;[End Block]
									Case 9
										;[Block]
										TempStr = TempStr + "682"
										;[End Block]
									Case 10
										;[Block]
										TempStr = TempStr + "714"
										;[End Block]
									Case 11
										;[Block]
										TempStr = TempStr + "860"
										;[End Block]
									Case 12
										;[Block]
										TempStr = TempStr + "860-1"
										;[End Block]
									Case 13
										;[Block]
										TempStr = TempStr + "895"
										;[End Block]
									Case 14
										;[Block]
										TempStr = TempStr + "939"
										;[End Block]
									Case 15
										;[Block]
										TempStr = TempStr + "966"
										;[End Block]
									Case 16
										;[Block]
										TempStr = TempStr + "970"
										;[End Block]
									Case 17
										;[Block]
										TempStr = TempStr + "1048"
										;[End Block]
									Case 18
										;[Block]
										TempStr = TempStr + "1162"
										;[End Block]
									Case 19
										;[Block]
										TempStr = TempStr + "1499"
										;[End Block]
								End Select
								TempStr2 = "paper"
								;[End Block]
							Case (Chance >= 40) And (Chance < 45) ; ~ 5% chance for a key card
								;[Block]
								Temp3% = Rand(1, 2)
								TempStr = "Level " + Str(Temp3) + " Key Card"
								TempStr2 = "key" + Str(Temp3)
								;[End Block]
							Case (Chance >= 45) And (Chance < 50) ; ~ 5% chance for a medkit
								;[Block]
								TempStr = "First Aid Kit"
								TempStr2 = "firstaid"
								;[End Block]
							Case (Chance >= 50) And (Chance < 60) ; ~ 10% chance for a battery
								;[Block]
								TempStr = "9V Battery"
								TempStr2 = "bat"
								;[End Block]
							Case (Chance >= 60) And (Chance < 70) ; ~ 10% chance for an SNAV
								;[Block]
								TempStr = "S-NAV 300 Navigator"
								TempStr2 = "nav"
								;[End Block]
							Case (Chance >= 70) And (Chance < 85) ; ~ 15% chance for a radio
								;[Block]
								TempStr = "Radio Transceiver"
								TempStr2 = "radio"
								;[End Block]
							Case (Chance >= 85) And (Chance < 95) ; ~ 10% chance for a clipboard
								;[Block]
								TempStr = "Clipboard"
								TempStr2 = "clipboard"
								;[End Block]
							Case (Chance >= 95) And (Chance =< 100) ; ~ 5% chance for misc
								;[Block]
								Temp3% = Rand(1, 3)
								Select Temp3
									Case 1 ; ~ Playing card
										;[Block]
										TempStr = "Playing Card"
										;[End Block]
									Case 2 ; ~ Mastercard
										;[Block]
										TempStr = "Mastercard"
										;[End Block]
									Case 3 ; ~ Origami
										;[Block]
										TempStr = "Origami"
										;[End Block]
								End Select
								TempStr2 = "misc"
								;[End Block]
						End Select
						iX = (-672.0) + 864.0 * xTemp
						iy = 96.0 + 96.0 * yTemp
						iZ = 480.0 - 352.0 * zTemp + Rnd(-96.0, 96.0)
						
						it = CreateItem(TempStr, TempStr2, r\x + iX * RoomScale, r\y + iy * RoomScale, r\z + iZ * RoomScale)
						EntityParent(it\Collider, r\OBJ)							
					Next
				Next
			Next
			;[End Block]
		Case "room1123"
			;[Block]
			; ~ Fake door to the contianment chamber itself
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 832.0 * RoomScale, r\y + 512.0 * RoomScale, r\z + 368.0 * RoomScale, 0.0, r, False, 4, 3)
			r\RoomDoors[1]\Open = True : r\RoomDoors[1]\Locked = True : r\RoomDoors[1]\AutoClose = False
			PositionEntity(r\RoomDoors[1]\Buttons[0], EntityX(r\RoomDoors[1]\Buttons[0], True) - 0.12, EntityY(r\RoomDoors[1]\Buttons[0], True), EntityZ(r\RoomDoors[1]\buttons[0], True) + 0.061, True)
			PositionEntity(r\RoomDoors[1]\Buttons[1], EntityX(r\RoomDoors[1]\Buttons[1], True) + 0.12, EntityY(r\RoomDoors[1]\Buttons[1], True), EntityZ(r\RoomDoors[1]\buttons[1], True) - 0.061, True)
			
			; ~ Door to the containment chamber itself
			d = CreateDoor(r\Zone, r\x + 832.0 * RoomScale, r\y, r\z + 368.0 * RoomScale, 0.0, r, False, 4, 3)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.12, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) + 0.061, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.12, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.061, True)
			
			; ~ Door to the pre-containment chamber
			d = CreateDoor(r\Zone, r\x + 280.0 * RoomScale, r\y, r\z - 607.0 * RoomScale, 90.0, r)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.031, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			; ~ Fake door to the pre-containment chamber
			d = CreateDoor(r\Zone, r\x + 280.0 * RoomScale, r\y + 512.0 * RoomScale, r\z - 607.0 * RoomScale, 90.0, r)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			r\RoomDoors[0] = d	
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x + 832.0 * RoomScale, r\y + 166.0 * RoomScale, r\z + 784.0 * RoomScale)
			
			r\Objects[4] = CreatePivot()
			PositionEntity(r\Objects[4], r\x -648.0 * RoomScale, r\y + 592.0 * RoomScale, r\z + 692.0 * RoomScale)
			
			r\Objects[5] = CreatePivot()
			PositionEntity(r\Objects[5], r\x + 828.0 * RoomScale, r\y + 592.0 * RoomScale, r\z + 592.0 * RoomScale)
			
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x - 76.0 * RoomScale, r\y + 620.0 * RoomScale, r\z + 744.0 * RoomScale)
			
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x - 640.0 * RoomScale, r\y + 620.0 * RoomScale, r\z - 864.0 * RoomScale)	
			
			r\Objects[8] = CopyEntity(o\DoorModelID[8])
			PositionEntity(r\Objects[8], r\x - 272.0 * RoomScale, r\y + 512.0 * RoomScale, r\z + 288.0 * RoomScale)
			RotateEntity(r\Objects[8], 0.0, 90.0, 0.0)
			ScaleEntity(r\Objects[8], 45.0 * RoomScale, 45.0 * RoomScale, 80.0 * RoomScale)
			
			r\Objects[9] = CopyEntity(o\DoorModelID[9])
			PositionEntity(r\Objects[9],r\x - 272.0 * RoomScale, r\y + 512.0 * RoomScale, r\z + (288.0 - 70.0) * RoomScale)
			RotateEntity(r\Objects[9], 0.0, 10.0, 0.0)
			EntityType(r\Objects[9], HIT_MAP)
			ScaleEntity(r\Objects[9], 46.0 * RoomScale, 45.0 * RoomScale, 46.0 * RoomScale)
			
			r\Objects[10] = CopyEntity(r\Objects[8])
			PositionEntity(r\Objects[10], r\x - 272.0 * RoomScale, r\y + 512.0 * RoomScale, r\z + 736.0 * RoomScale)
			RotateEntity(r\Objects[10], 0.0, 90.0, 0.0)
			ScaleEntity(r\Objects[10], 45.0 * RoomScale, 45.0 * RoomScale, 80.0 * RoomScale)
			
			r\Objects[11] =  CopyEntity(r\Objects[9])
			PositionEntity(r\Objects[11], r\x - 272.0 * RoomScale, r\y + 512.0 * RoomScale, r\z + (736.0 - 70.0) * RoomScale)
			RotateEntity(r\Objects[11], 0.0, 90.0, 0.0)
			EntityType(r\Objects[11], HIT_MAP)
			ScaleEntity(r\Objects[11], 46.0 * RoomScale, 45.0 * RoomScale, 46.0 * RoomScale)
			
			r\Objects[12] = CopyEntity(r\Objects[8])
			PositionEntity(r\Objects[12], r\x - 592.0 * RoomScale, r\y + 512.0 * RoomScale, r\z - 704.0 * RoomScale)
			RotateEntity(r\Objects[12], 0.0, 0.0, 0.0)
			ScaleEntity(r\Objects[12], 45.0 * RoomScale, 45.0 * RoomScale, 80.0 * RoomScale)
			
			r\Objects[13] =  CopyEntity(r\Objects[9])
			PositionEntity(r\Objects[13],r\x - (592.0 + 70.0) * RoomScale, r\y + 512.0 * RoomScale, r\z - 704.0 * RoomScale)
			RotateEntity(r\Objects[13], 0.0, 0.0, 0.0)
			EntityType(r\Objects[13], HIT_MAP)
			ScaleEntity(r\Objects[13], 46.0 * RoomScale, 45.0 * RoomScale, 46.0 * RoomScale)	
			
			For i = 3 To 13
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[14] = LoadMesh_Strict("GFX\map\1123_hb.b3d", r\OBJ)
			EntityPickMode(r\Objects[14], 2)
			EntityType(r\Objects[14], HIT_MAP)
			EntityAlpha(r\Objects[14], 0.0)
			
			it = CreateItem("Document SCP-1123", "paper", r\x + 511.0 * RoomScale, r\y + 125.0 * RoomScale, r\z - 936.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("SCP-1123", "scp1123", r\x + 832.0 * RoomScale, r\y + 166.0 * RoomScale, r\z + 784.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, 90.0, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Leaflet", "paper", r\x - 816.0 * RoomScale, r\y + 704.0 * RoomScale, r\z+ 888.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Gas Mask", "gasmask", r\x + 457.0 * RoomScale, r\y + 150.0 * RoomScale, r\z + 960.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "pocketdimension"
			;[Block]
			; ~ Doors inside fake tunnel
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x, r\y + 2048.0 * RoomScale, r\z + 32.0 - 1024.0 * RoomScale, 0.0, r)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x, r\y + 2048.0 * RoomScale, r\z + 32.0 + 1024.0 * RoomScale, 180.0, r)
			
			Local Entity%
			Local Hallway% = LoadMesh_Strict("GFX\map\pocketdimension2.b3d") ; ~ The tunnels in the first room
			
			r\Objects[8] = LoadMesh_Strict("GFX\map\pocketdimension3.b3d")	; ~ The room with the throne, moving pillars etc 
			
			r\Objects[9] = LoadMesh_Strict("GFX\map\pocketdimension4.b3d") ; ~ The flying pillar
			
			r\Objects[10] = CopyEntity(r\Objects[9])
			ScaleEntity(r\Objects[10], RoomScale * 1.5, RoomScale * 2.0, RoomScale * 1.5, True)	
			
			r\Objects[11] = LoadMesh_Strict("GFX\map\pocketdimension5.b3d") ; ~ The pillar room			PositionEntity(r\Objects[11], r\x, r\y, r\z + 64.0, True)	
			
			Local Terrain% = LoadMesh_Strict("GFX\map\pocketdimensionterrain.b3d")
			
			ScaleEntity(Terrain, RoomScale, RoomScale, RoomScale, True)
			PositionEntity(Terrain, r\x, r\y + 29440.0, r\z, True)
			
			For k = 0 To -1
				Select k
					Case 0
						;[Block]
						Entity = Hallway 
						;[End Block]
					Case 1
						;[Block]
						Entity = r\Objects[8]
						;[End Block]
					Case 2
						;[Block]
						Entity = r\Objects[9]
						;[End Block]
					Case 3
						;[Block]
						Entity = r\Objects[10]
						;[End Block]
					Case 4
						;[Block]
						Entity = r\Objects[11]
						;[End Block]
				End Select 
			Next
			
			For i = 8 To 11
				ScaleEntity(r\Objects[i], RoomScale, RoomScale, RoomScale)
				EntityType(r\Objects[i], HIT_MAP)
				EntityPickMode(r\Objects[i], 2)
				PositionEntity(r\Objects[i], r\x, r\y, r\z + 32.0, True)
			Next
			
			ScaleEntity(Terrain, RoomScale, RoomScale, RoomScale)
			EntityType(Terrain, HIT_MAP)
			EntityPickMode(Terrain, 3)
			PositionEntity(Terrain, r\x, r\y + 2944.0 * RoomScale, r\z + 32.0, True)			
			
			de = CreateDecal(18, r\x - (1536.0 * RoomScale), r\y + 0.02, r\z + 608.0 * RoomScale + 32.0, 90.0, 0.0, 0.0)
			de\Size = Rnd(0.8, 0.8) : de\BlendMode = 2 : de\FX = 1 + 8
			ScaleSprite(de\OBJ, de\Size, de\Size)
			EntityParent(de\OBJ, r\OBJ)
			
			For i = 1 To 8
				r\Objects[i - 1] = CopyEntity(Hallway)
				
				Angle# = (i - 1) * (360.0 / 8.0)
				
				ScaleEntity(r\Objects[i - 1], RoomScale, RoomScale, RoomScale)
				EntityType(r\Objects[i - 1], HIT_MAP)
				EntityPickMode(r\Objects[i - 1], 2)
				RotateEntity(r\Objects[i - 1], 0.0, Angle - 90.0, 0.0)
				PositionEntity(r\Objects[i - 1], r\x + Cos(Angle) * (512.0 * RoomScale), r\y, r\z + Sin(Angle) * (512.0 * RoomScale))
				EntityParent(r\Objects[i - 1], r\OBJ)
				
				If i < 6 Then 
					de = CreateDecal(i + 7, r\x + Cos(Angle) * (512.0 * RoomScale) * 3.0, r\y + 0.02, r\z + Sin(Angle) * (512.0 * RoomScale) * 3.0, 90.0, Angle - 90.0, 0.0)
					de\Size = Rnd(0.5, 0.5) : de\BlendMode = 2 : de\FX = 1 + 8
					EntityBlend(de\OBJ, 2)
				EndIf				
			Next
			
			For i = 12 To 16
				r\Objects[i] = CreatePivot(r\Objects[11])
				Select i
					Case 12
						;[Block]
						PositionEntity(r\Objects[i], r\x, r\y + 200.0 * RoomScale, r\z + 64.0, True)
						;[End Block]
					Case 13
						;[Block]
						PositionEntity(r\Objects[i], r\x + 390.0 * RoomScale, r\y + 200.0 * RoomScale, r\z + 64.0 + 272.0 * RoomScale, True)
						;[End Block]
					Case 14
						;[Block]
						PositionEntity(r\Objects[i], r\x + 838.0 * RoomScale, r\y + 200.0 * RoomScale, r\z + 64.0 - 551.0 * RoomScale, True)	
						;[End Block]
					Case 15
						;[Block]
						PositionEntity(r\Objects[i], r\x - 139.0 * RoomScale, r\y + 200.0 * RoomScale, r\z + 64.0 + 1201.0 * RoomScale, True)
						;[End Block]
					Case 16
						;[Block]
						PositionEntity(r\Objects[i], r\x - 1238.0 * RoomScale, r\y - 1664.0 * RoomScale, r\z + 64.0 + 381.0 * RoomScale, True)
						;[End Block]
				End Select 
			Next
			
			Local OldManEyes% = LoadTexture_Strict("GFX\npcs\scp_106_eyes.png")
			
			r\Objects[17] = CreateSprite()
			ScaleSprite(r\Objects[17], 0.03, 0.03)
			EntityTexture(r\Objects[17], OldManEyes)
			EntityBlend (r\Objects[17], 3)
			EntityFX(r\Objects[17], 1 + 8)
			SpriteViewMode(r\Objects[17], 2)
			
			r\Objects[18] = LoadTexture_Strict("GFX\npcs\pd_plane.png", 1 + 2)
			
			r\Objects[19] = LoadTexture_Strict("GFX\npcs\pd_plane_eye.png", 1 + 2)
			
			r\Objects[20] = CreateSprite()
			ScaleSprite(r\Objects[20], 8.0, 8.0)
			EntityTexture(r\Objects[20], r\Objects[18])
			EntityOrder(r\Objects[20], 100)
			EntityBlend(r\Objects[20], 2)
			EntityFX(r\Objects[20], 1 + 8)
			SpriteViewMode(r\Objects[20], 2)
			FreeTexture(t)
			FreeEntity(Hallway)
			
			it = CreateItem("Burnt Note", "paper", EntityX(r\OBJ), r\y + 0.5, EntityZ(r\OBJ) + 3.5)
			;[End Block]
		Case "room3z3"
			;[Block]
			sc = CreateSecurityCam(r\x - 320.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 512.25 * RoomScale, r)
			sc\Angle = 225.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			;[End Block]
		Case "room2_3", "room3_3"
			;[Block]
			w.WayPoints = CreateWaypoint(r\x, r\y + 66.0 * RoomScale, r\z, Null, r)
			;[End Block]
		Case "room1lifts"
			;[Block]
			r\Objects[0] = CreateButton(r\x + 96.0 * RoomScale, r\y + 160.0 * RoomScale, r\z + 71.0 * RoomScale, 0.0, 0.0, 0.0)
			
			r\Objects[1] = CreateButton(r\x - 96.0 * RoomScale, r\y + 160.0 * RoomScale, r\z + 71.0 * RoomScale, 0.0, 0.0, 0.0)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			sc = CreateSecurityCam(r\x + 384.0 * RoomScale, r\y + (448.0 - 64.0) * RoomScale, r\z - 960.0 * RoomScale, r, True)
			sc\Angle = 45.0 : sc\Turn = 45.0 : sc\room = r
			TurnEntity(sc\CameraOBJ, 20, 0, 0)
			EntityParent(sc\OBJ, r\OBJ)
			
			w.WayPoints = CreateWaypoint(r\x, r\y + 66.0 * RoomScale, r\z, Null, r)
			;[End Block]
		Case "room2servers2"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 264.0 * RoomScale, r\y, r\z + 672.0 * RoomScale, 270.0, r, False, False, 3)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.031, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)	
			RotateEntity(d\Buttons[1], 0.0, 0.0, 0.0, True)
			
			d = CreateDoor(r\Zone, r\x - 512.0 * RoomScale, r\y - 768.0 * RoomScale, r\z - 336.0 * RoomScale, 0.0, r, False, False, 3)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) + 0.061, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.061, True)	
			
			d = CreateDoor(r\Zone, r\x - 509.0 * RoomScale, r\y - 768.0 * RoomScale, r\z - 1037.0 * RoomScale, 0.0, r, False, False, 3)
			d\Locked = True : d\DisableWaypoint = True : d\MTFClose = False
			FreeEntity(d\Buttons[0]) : d\Buttons[0] = 0
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.012, True)
			
			it = CreateItem("Night Vision Goggles", "nvgoggles", r\x + 56.0154 * RoomScale, r\y - 648.0 * RoomScale, r\z + 749.638 * RoomScale)
			it\State = 200.0
			RotateEntity(it\Collider, 0.0, r\Angle + Rand(245), 0.0)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2gw", "room2gw_b"
		    ;[Block]
			If r\RoomTemplate\Name = "room2gw_b"
				r\Objects[0] = CreatePivot()
				PositionEntity(r\Objects[0], r\x + 280.0 * RoomScale, r\y + 345.0 * RoomScale, r\z - 340.0 * RoomScale)
				EntityParent(r\Objects[0], r\OBJ)
				
				r\Objects[1] = CreatePivot()
				PositionEntity(r\Objects[1], r\x - 156.825 * RoomScale, r\y + 0.5, r\z + 121.364 * RoomScale)
				EntityParent(r\Objects[1], r\OBJ)
			EndIf
			
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 336.0 * RoomScale, r\y, r\z - 382.0 * RoomScale, 0.0, r)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\Open = True  : r\RoomDoors[0]\Locked = True : r\RoomDoors[0]\MTFClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[0]\Buttons[i]) : r\RoomDoors[0]\Buttons[i] = 0
			Next
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 336.0 * RoomScale, r\y, r\z + 462.0 * RoomScale, 180.0, r)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\Open = True  : r\RoomDoors[1]\Locked = True : r\RoomDoors[1]\MTFClose = False
			
			For i = 0 To 1
				FreeEntity(r\RoomDoors[1]\Buttons[i]) : r\RoomDoors[1]\Buttons[i] = 0
			Next
			
			For r2.Rooms = Each Rooms
				If r2 <> r Then
					If r2\RoomTemplate\Name = "room2gw" Or r2\RoomTemplate\Name = "room2gw_b" Then
						r\Objects[2] = CopyEntity(r2\Objects[2], r\OBJ) ; ~ Don't load the mesh again
						Exit
					EndIf
				EndIf
			Next
			If r\Objects[2] = 0 Then r\Objects[2] = LoadMesh_Strict("GFX\map\room2gw_pipes.b3d", r\OBJ)
			EntityPickMode(r\Objects[2], 2)
			
			If r\RoomTemplate\Name = "room2gw"
				r\Objects[0] = CreatePivot()
				PositionEntity(r\Objects[0], r\x + 344.0 * RoomScale, r\y + 128.0 * RoomScale, r\z)
				EntityParent(r\Objects[0], r\OBJ)
				
				Local BD_Temp% = False
				
				If room2gw_BrokenDoor
					If room2gw_x = r\x
						If room2gw_z = r\z
							BD_Temp = True
						EndIf
					EndIf
				EndIf
				
				If (room2gw_BrokenDoor = 0 And Rand(2) = 1) Or BD_Temp
					r\Objects[1] = CopyEntity(o\DoorModelID[0])
					ScaleEntity(r\Objects[1], (204.0 * RoomScale) / MeshWidth(r\Objects[1]), 312.0 * RoomScale / MeshHeight(r\Objects[1]), 16.0 * RoomScale / MeshDepth(r\Objects[1]))
					EntityType(r\Objects[1], HIT_MAP)
					PositionEntity(r\Objects[1], r\x + 336.0 * RoomScale, r\y, r\z + 462.0 * RoomScale)
					RotateEntity(r\Objects[1], 0.0, 180.0 + 180.0, 0.0)
					EntityParent(r\Objects[1], r\OBJ)
					MoveEntity(r\Objects[1], 120.0, 0.0, 5.0)
					
					room2gw_BrokenDoor = True
					room2gw_x = r\x
					room2gw_z = r\z
					
					FreeEntity(r\RoomDoors[1]\OBJ2) : r\RoomDoors[1]\OBJ2 = 0
				EndIf
			EndIf
			;[End Block]
		Case "room1162"
			;[Block]
			d = CreateDoor(r\Zone, r\x + 248.0 * RoomScale, r\y, r\z - 736.0 * RoomScale, 90.0, r, False, False, 2)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.031, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 1012.0 * RoomScale, r\y + 128.0 * RoomScale, r\z - 640.0 * RoomScale)
			EntityPickMode(r\Objects[0], 1)
			EntityParent(r\Objects[0], r\OBJ)
			
			sc = CreateSecurityCam(r\x - 192.0 * RoomScale, r\y + 704.0 * RoomScale, r\z + 192.0 * RoomScale, r)
			sc\Angle = 225.0 : sc\Turn = 45.0
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			
			it = CreateItem("Document SCP-1162", "paper", r\x + 863.227 * RoomScale, r\y + 152.0 * RoomScale, r\z - 953.231 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2scps2"
			;[Block]
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 288.0 * RoomScale, r\y, r\z + 576.0 * RoomScale, 90.0, r, False, False, 3)
			r\RoomDoors[0]\Open = False : r\RoomDoors[0]\Locked = True
			
			d = CreateDoor(r\Zone, r\x + 777.0 * RoomScale, r\y, r\z + 671.0 * RoomScale, 90.0, r, False, False, 4)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.02, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.02, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			d = CreateDoor(r\Zone, r\x + 556.0 * RoomScale, r\y, r\z + 296.0 * RoomScale, 0.0, r, False, False, 3)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True), EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True) + 0.031, True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True), EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True) - 0.031, True)
			
			r\Objects[0] = CreatePivot()
			PositionEntity(r\Objects[0], r\x + 576.0 * RoomScale, r\y + 160.0 * RoomScale, r\z + 632.0 * RoomScale)
			EntityParent(r\Objects[0], r\OBJ)
			
			For i = 0 To 1
				Select i
					Case 0
						;[Block]
						scX = 850.0
						scY = 352.0
						scZ = 876.0
						scAngle = 220.0
						;[End Block]
					Case 1
						;[Block]
						scX = 600.0
						scY = 512.0
						scZ = 150.0
						scAngle = 180.0
						;[End Block]
				End Select
				sc = CreateSecurityCam(r\x + scX * RoomScale, r\y + scY * RoomScale, r\z + scZ * RoomScale, r)
				sc\Angle = scAngle : sc\Turn = 30.0
				TurnEntity(sc\CameraOBJ, 30.0, 0.0, 0.0)
				EntityParent(sc\OBJ, r\OBJ)
			Next
			
			it = CreateItem("SCP-1499", "scp1499", r\x + 600.0 * RoomScale, r\y + 176.0 * RoomScale, r\z - 228.0 * RoomScale)
			RotateEntity(it\Collider, 0.0, r\Angle, 0.0)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-1499", "paper", r\x + 840.0 * RoomScale, r\y + 260.0 * RoomScale, r\z + 224.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Document SCP-500", "paper", r\x + 1152.0 * RoomScale, r\y + 224.0 * RoomScale, r\z + 336.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Emily Ross' Badge", "badge", r\x + 364.0 * RoomScale, r\y + 5.0 * RoomScale, r\z + 716.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room3offices"
			;[Block]			
			d = CreateDoor(r\Zone, r\x + 736.0 * RoomScale, r\y, r\z + 240.0 * RoomScale, 0.0, r, False, 4, 3)
			PositionEntity(d\Buttons[0], r\x + 892.0 * RoomScale, EntityY(d\Buttons[0], True), r\z + 226.0 * RoomScale, True)
			PositionEntity(d\Buttons[1], r\x + 892.0 * RoomScale, EntityY(d\Buttons[1], True), r\z + 253.0 * RoomScale, True)
			
			r\Objects[0] = LoadMesh_Strict("GFX\map\room3offices_hb.b3d", r\OBJ)
			EntityPickMode(r\Objects[0], 2)
			EntityType(r\Objects[0], HIT_MAP)
			EntityAlpha(r\Objects[0], 0.0)
			;[End Block]
		Case "room2offices4"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 240.0 * RoomScale, r\y, r\z, 90.0, r)
			d\Open = False : d\AutoClose = False
			PositionEntity(d\Buttons[0], r\x - 230.0 * RoomScale, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], r\x - 250.0 * RoomScale, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			it = CreateItem("Sticky Note", "paper", r\x - 991.0 * RoomScale, r\y - 242.0 * RoomScale, r\z + 904.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2sl"
			;[Block]
			; ~ Doors for room
			r\RoomDoors[0] = CreateDoor(r\Zone, r\x + 480.0 * RoomScale, r\y, r\z - 640.0 * RoomScale, 90.0, r, False, False, 3)
			r\RoomDoors[0]\AutoClose = False : r\RoomDoors[0]\MTFClose = False
			PositionEntity(r\RoomDoors[0]\Buttons[0], r\x + 576.0 * RoomScale, EntityY(r\RoomDoors[0]\Buttons[0], True), r\z - 474.0 * RoomScale, True)
			RotateEntity(r\RoomDoors[0]\Buttons[0], 0.0, 270.0, 0.0)
			
			r\RoomDoors[1] = CreateDoor(r\Zone, r\x + 544.0 * RoomScale, r\y + 480.0 * RoomScale, r\z + 256.0 * RoomScale, 270.0, r, False, 4, 3)
			r\RoomDoors[1]\AutoClose = False : r\RoomDoors[1]\MTFClose = False
			
			d = CreateDoor(r\Zone, r\x + 1504.0 * RoomScale, r\y + 480.0 * RoomScale, r\z + 960.0 * RoomScale, 0.0, r)
			d\AutoClose = False : d\Locked = True : d\MTFClose = False
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			Local Scale# = RoomScale * 4.5 * 0.4
			Local Screen%
			
			r\Textures[0] = LoadAnimTexture("GFX\SL_monitors_checkpoint.jpg", 1, 512, 512, 0, 4)
			r\Textures[1] = LoadAnimTexture("GFX\Sl_monitors.jpg", 1, 256, 256, 0, 8)
			
			; ~ Monitor Objects
			For i = 0 To 14
				If i <> 7 Then
					r\Objects[i] = CopyEntity(o\MonitorModelID[0])
					ScaleEntity(r\Objects[i], Scale, Scale, Scale)
					If i <> 4 And i <> 13 Then
						Screen = CreateSprite()
						EntityFX(Screen, 17)
						SpriteViewMode(Screen, 2)
						ScaleSprite(Screen, MeshWidth(o\MonitorModelID[0]) * Scale * 0.95 * 0.5, MeshHeight(o\MonitorModelID[0]) * Scale * 0.95 * 0.5)
						Select i
							Case 0
								;[Block]
								EntityTexture(Screen, r\Textures[1], 0)
								;[End Block]
							Case 2
								;[Block]
								EntityTexture(Screen, r\Textures[1], 2)
								;[End Block]
							Case 3
								;[Block]
								EntityTexture(Screen, r\Textures[1], 1)
								;[End Block]
							Case 8
								;[Block]
								EntityTexture(Screen, r\Textures[1], 4)
								;[End Block]
							Case 9
								;[Block]
								EntityTexture(Screen, r\Textures[1], 5)
								;[End Block]
							Case 10
								;[Block]
								EntityTexture(Screen, r\Textures[1], 3)
								;[End Block]
							Case 11
								;[Block]
								EntityTexture(Screen, r\Textures[1], 7)
								;[End Block]
							Default
								;[Block]
								EntityTexture(Screen, r\Textures[0], 3)
								;[End Block]
						End Select
						EntityParent(Screen, r\Objects[i])
					ElseIf i = 4 Then
						r\Objects[20] = CreateSprite()
						EntityFX(r\Objects[20], 17)
						SpriteViewMode r\Objects[20], 2
						ScaleSprite(r\Objects[20], MeshWidth(o\MonitorModelID[0]) * Scale * 0.95 * 0.5, MeshHeight(o\MonitorModelID[0]) * Scale * 0.95 * 0.5)
						EntityTexture(r\Objects[20], r\Textures[0], 2)
						EntityParent(r\Objects[20], r\Objects[i])
					Else
						r\Objects[21] = CreateSprite()
						EntityFX(r\Objects[21], 17)
						SpriteViewMode(r\Objects[21], 2)
						ScaleSprite(r\Objects[21], MeshWidth(o\MonitorModelID[0]) * Scale * 0.95 * 0.5, MeshHeight(o\MonitorModelID[0]) * Scale * 0.95 * 0.5)
						EntityTexture(r\Objects[21], r\Textures[1], 6)
						EntityParent(r\Objects[21], r\Objects[i])
					EndIf
				EndIf
			Next
			
			For i = 0 To 2
				PositionEntity(r\Objects[i], r\x - 207.94 * RoomScale, r\y + (648.0 + (112.0 * i)) * RoomScale, r\z - 60.0686 * RoomScale)
				RotateEntity(r\Objects[i], 0.0, 105.0 + r\Angle, 0.0)
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For i = 3 To 5
				PositionEntity(r\Objects[i], r\x - 231.489 * RoomScale, r\y + (648.0 + (112.0 * (i - 3))) * RoomScale, r\z + 95.7443 * RoomScale)
				RotateEntity(r\Objects[i], 0.0, 90.0 + r\Angle, 0.0)
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For i = 6 To 8 Step 2
				PositionEntity(r\Objects[i], r\x - 231.489 * RoomScale, r\y + (648.0 + (112.0 * (i - 6))) * RoomScale, r\z + 255.744 * RoomScale)
				RotateEntity(r\Objects[i], 0.0, 90.0 + r\Angle, 0.0)
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For i = 9 To 11
				PositionEntity(r\Objects[i], r\x - 231.489 * RoomScale, r\y + (648.0 + (112.0 * (i - 9))) * RoomScale, r\z + 415.744 * RoomScale)
				RotateEntity(r\Objects[i], 0.0, 90.0 + r\Angle, 0.0)
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			For i = 12 To 14
				PositionEntity(r\Objects[i], r\x - 208.138 * RoomScale, r\y + (648.0 + (112.0 * (i - 12))) * RoomScale, r\z + 571.583 * RoomScale)
				RotateEntity(r\Objects[i], 0.0, 75.0 + r\Angle, 0.0)
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			; ~ PathPoint 1 for SCP-049
			r\Objects[7] = CreatePivot()
			PositionEntity(r\Objects[7], r\x, r\y + 100.0 * RoomScale, r\z - 800.0 * RoomScale, True)
			EntityParent(r\Objects[7], r\OBJ)
			
			; ~ PathPoints for SCP-049
			r\Objects[15] = CreatePivot()
			PositionEntity(r\Objects[15], r\x + 700.0 * RoomScale, r\y + 700.0 * RoomScale, r\z + 256.0 * RoomScale)
			
			r\Objects[16] = CreatePivot()
			PositionEntity(r\Objects[16], r\x - 60.0 * RoomScale, r\y + 700.0 * RoomScale, r\z + 200.0 * RoomScale)
			
			r\Objects[17] = CreatePivot()
			PositionEntity(r\Objects[17], r\x - 48.0 * RoomScale, r\y + 540.0 * RoomScale, r\z + 656.0 * RoomScale)
			
			For i = 15 To 17
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			r\Objects[9 * 2] = CopyEntity(o\LeverModelID[0])
			r\Objects[9 * 2 + 1] = CopyEntity(o\LeverModelID[1])
			
			r\Levers[0] = r\Objects[9 * 2 + 1]
			
			For i = 0 To 1
				ScaleEntity(r\Objects[9 * 2 + i], 0.04, 0.04, 0.04)
				PositionEntity(r\Objects[9 * 2 + i], r\x - 49.0 * RoomScale, r\y + 689.0 * RoomScale, r\z + 912.0 * RoomScale)
				EntityParent(r\Objects[9 * 2 + i], r\OBJ)
			Next
			
			RotateEntity(r\Objects[9 * 2], 0.0, 0.0, 0.0)
			RotateEntity(r\Objects[9 * 2 + 1], 10.0, 0.0 - 180.0, 0.0)
			EntityPickMode(r\Objects[9 * 2 + 1], 1, False)
			EntityRadius(r\Objects[9 * 2 + 1], 0.1)
			
			; ~ Camera in the room itself
			sc = CreateSecurityCam(r\x - 159.0 * RoomScale, r\y + 384.0 * RoomScale, r\z - 929.0 * RoomScale, r, True)
			sc\Angle = 315.0 : sc\room = r
			TurnEntity(sc\CameraOBJ, 20.0, 0.0, 0.0)
			EntityParent(sc\OBJ, r\OBJ)
			PositionEntity(sc\ScrOBJ, r\x - 231.489 * RoomScale, r\y + 760.0 * RoomScale, r\z + 255.744 * RoomScale)
			TurnEntity(sc\ScrOBJ, 0.0, 90.0, 0.0)
			EntityParent(sc\ScrOBJ, r\OBJ)
			;[End Block]
		Case "room2_4"
			;[Block]
			r\Objects[6] = CreatePivot()
			PositionEntity(r\Objects[6], r\x + 640.0 * RoomScale, r\y + 8.0 * RoomScale, r\z - 896.0 * RoomScale)
			EntityParent(r\Objects[6], r\OBJ)
			;[End Block]
		Case "room3z2"
			;[Block]
			For r2.Rooms = Each Rooms
				If r2\RoomTemplate\Name = r\RoomTemplate\Name And r2 <> r
					r\Objects[0] = CopyEntity(r2\Objects[0], r\OBJ) ; ~ Don't load the mesh again
					Exit
				EndIf
			Next
			If r\Objects[0] = 0 Then r\Objects[0] = LoadMesh_Strict("GFX\map\room3z2_hb.b3d", r\OBJ)
			EntityPickMode(r\Objects[0], 2)
			EntityType(r\Objects[0], HIT_MAP)
			EntityAlpha(r\Objects[0], 0.0)
			;[End Block]
		Case "room2clockroom2"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 736.0 * RoomScale, r\y, r\z - 104.0 * RoomScale, 0.0, r, True)
			d\Timer = 70 * 5.0 : d\AutoClose = False : d\Open = False : d\Locked = True
			PositionEntity(d\Buttons[0], r\x - 288.0 * RoomScale, EntityY(d\Buttons[0], True), r\z - 634.0 * RoomScale, True)
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			d2 = CreateDoor(r\Zone, r\x + 104.0 * RoomScale, r\y, r\z + 736.0 * RoomScale, 270.0, r, True)
			d2\Timer = 70 * 5.0 : d2\AutoClose = False : d2\Open = False : d2\locked = True
			PositionEntity(d2\Buttons[0], r\x + 634.0 * RoomScale, EntityY(d2\Buttons[0], True), r\z + 288.0 * RoomScale, True)
			RotateEntity(d2\Buttons[0], 0.0, 90.0, 0.0, True)
			FreeEntity(d2\Buttons[1]) : d2\Buttons[1] = 0
			
			d\LinkedDoor = d2
			d2\LinkedDoor = d
			
			Scale = RoomScale * 4.5 * 0.4
			
			r\Objects[0] = CopyEntity(o\MonitorModelID[0])
			ScaleEntity(r\Objects[0], Scale, Scale, Scale)
			PositionEntity(r\Objects[0], r\x + 668.0 * RoomScale, r\y + 1.1, r\z - 96.0 * RoomScale)
			RotateEntity(r\Objects[0], 0.0, 90.0, 0.0)
			
			r\Objects[1] = CopyEntity(o\MonitorModelID[0])
			ScaleEntity(r\Objects[1], Scale, Scale, Scale)
			PositionEntity(r\Objects[1], r\x + 96.0 * RoomScale, r\y + 1.1, r\z - 668.0 * RoomScale)
			
			For i = 0 To 1
				EntityParent(r\Objects[i], r\OBJ)
			Next
			;[End Block]
		Case "medibay"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 264.0 * RoomScale, r\y, r\z + 640.0 * RoomScale, 90.0, r, False, False, 3)
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.031, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			PositionEntity(d\Buttons[1], EntityX(d\Buttons[1], True) + 0.031, EntityY(d\Buttons[1], True), EntityZ(d\Buttons[1], True), True)
			
			r\Objects[0] = LoadMesh_Strict("GFX\map\medibay_props.b3d", r\OBJ)
			EntityType(r\Objects[0], HIT_MAP)
			EntityPickMode(r\Objects[0], 2)
			
			r\Objects[1] = CreatePivot()
			PositionEntity(r\Objects[1], r\x - 762.0 * RoomScale, r\y, r\z - 346.0 * RoomScale)
			
			r\Objects[2] = CreatePivot()
			PositionEntity(r\Objects[2], EntityX(r\Objects[1], True) + 126.0 * RoomScale, EntityY(r\Objects[1], True), EntityZ(r\Objects[1], True))
			
			r\Objects[3] = CreatePivot()
			PositionEntity(r\Objects[3], r\x - 820.0 * RoomScale, r\y, r\z - 318.399 * RoomScale)
			
			For i = 1 To 3
				EntityParent(r\Objects[i], r\OBJ)
			Next
			
			it = CreateItem("First Aid Kit", "firstaid", r\x - 506.0 * RoomScale, r\y + 192.0 * RoomScale, r\z - 322.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Syringe", "syringe", r\x - 333.0 * RoomScale, r\y + 100.0 * RoomScale, r\z + 97.3 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			
			it = CreateItem("Syringe", "syringe", r\x - 340.0 * RoomScale, r\y + 100.0 * RoomScale, r\z + 52.3 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "room2cpit"
			;[Block]
			d = CreateDoor(r\Zone, r\x - 256.0 * RoomScale, r\y, r\z - 752.0 * RoomScale, 90.0, r, False, 2, 3)
            d\Locked = True : d\Open = False : d\AutoClose = False : d\MTFClose = False : d\DisableWaypoint = True
			PositionEntity(d\Buttons[0], EntityX(d\Buttons[0], True) - 0.061, EntityY(d\Buttons[0], True), EntityZ(d\Buttons[0], True), True)
			FreeEntity(d\Buttons[1]) : d\Buttons[1] = 0
			
			em = CreateEmitter(r\x + 512.0 * RoomScale, r\y - 76.0 * RoomScale, r\z - 688.0 * RoomScale, 0)
			em\RandAngle = 55.0 : em\Speed = 0.0005 : em\Achange = -0.015 : em\SizeChange = 0.007
            TurnEntity(em\OBJ, -90.0, 0.0, 0.0)
            EntityParent(em\OBJ, r\OBJ)
			
			it = CreateItem("Dr L's Note", "paper", r\x - 160.0 * RoomScale, r\y + 32.0 * RoomScale, r\z - 353.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
		Case "dimension1499"
			;[Block]
			r\Levers[0] = CreatePivot()
			PositionEntity(r\Levers[0], r\x + 205.0 * RoomScale, r\y + 200.0 * RoomScale, r\z + 2287.0 * RoomScale)
			EntityParent(r\Levers[0], r\OBJ)
			
			r\Levers[1] = LoadMesh_Strict("GFX\map\dimension1499\1499object0_cull.b3d", r\OBJ)
			EntityType(r\Levers[1], HIT_MAP)
			EntityAlpha(r\Levers[1], 0.0)
			;[End Block]
		Case "room4info"
			;[Block]
			r\Objects[0] = CopyEntity(o\MonitorModelID[1], r\OBJ)
			PositionEntity(r\Objects[0], r\x - 700.0 * RoomScale, r\y + 384.0 * RoomScale, r\z + 290.0 * RoomScale, True)
			ScaleEntity(r\Objects[0], 2.0, 2.0, 2.0)
			RotateEntity(r\Objects[0], 0.0, 0.0, 0.0)
			EntityFX(r\Objects[0], 1)
			
			it = CreateItem("Radio Transceiver", "fineradio", r\x + 650.0 * RoomScale, r\y + 258.0 * RoomScale, r\z - 760.0 * RoomScale)
			EntityParent(it\Collider, r\OBJ)
			;[End Block]
	End Select
	
	For lt.LightTemplates = Each LightTemplates
		If lt\RoomTemplate = r\RoomTemplate Then
			Newlt = AddLight(r, r\x + lt\x, r\y + lt\y, r\z + lt\z, lt\lType, lt\Range, lt\r, lt\g, lt\b)
			If Newlt <> 0 Then 
				If lt\ltype = 3 Then
					RotateEntity(Newlt, lt\Pitch, lt\Yaw, 0.0)
				EndIf
			EndIf
		EndIf
	Next
	
	For ts.TempScreens = Each TempScreens
		If ts\RoomTemplate = r\RoomTemplate Then
			CreateScreen(r\x + ts\x, r\y + ts\y, r\z + ts\z, ts\ImgPath, r)
		EndIf
	Next
	
	For tw.TempWayPoints = Each TempWayPoints
		If tw\roomtemplate = r\RoomTemplate Then
			CreateWaypoint(r\x + tw\x, r\y + tw\y, r\z + tw\z, Null, r)
		EndIf
	Next
	
	If r\RoomTemplate\TempTriggerboxAmount > 0
		r\TriggerboxAmount = r\RoomTemplate\TempTriggerboxAmount
		For i = 0 To r\TriggerboxAmount - 1
			r\Triggerbox[i] = CopyEntity(r\RoomTemplate\TempTriggerbox[i], r\OBJ)
			r\TriggerboxName[i] = r\RoomTemplate\TempTriggerboxName[i]
		Next
	EndIf
	
	For i = 0 To MaxRoomEmitters - 1
		If r\RoomTemplate\TempSoundEmitter[i] <> 0 Then
			r\SoundEmitterOBJ[i] = CreatePivot(r\OBJ)
			PositionEntity(r\SoundEmitterOBJ[i], r\x + r\RoomTemplate\TempSoundEmitterX[i], r\y + r\RoomTemplate\TempSoundEmitterY[i], r\z + r\RoomTemplate\TempSoundEmitterZ[i], True)
			EntityParent(r\SoundEmitterOBJ[i], r\OBJ)
			
			r\SoundEmitter[i] = r\RoomTemplate\TempSoundEmitter[i]
			r\SoundEmitterRange[i] = r\RoomTemplate\TempSoundEmitterRange[i]
		EndIf
	Next
	
	CatchErrors("FillRoom (" + r\RoomTemplate\Name + ")")
End Function

Function UpdateRooms()
	CatchErrors("Uncaught (UpdateRooms)")
	
	Local Dist#, i%, j%, r.Rooms
	Local x#, z#, Hide% = True
	
	; ~ The reason why it is like this:
	; ~ When the map gets spawned by a seed, it starts from LCZ to HCZ to EZ (bottom to top)
	; ~ A map loaded by the map creator starts from EZ to HCZ to LCZ (top to bottom) and that's why this little code thing with the (SelectedMap = "") needs to be there - ENDSHN
	If (EntityZ(Collider) / 8.0) < I_Zone\Transition[1] - (SelectedMap = "") Then
		PlayerZone = 2
	ElseIf (EntityZ(Collider) / 8.0) >= I_Zone\Transition[1] - (SelectedMap = "") And (EntityZ(Collider) / 8.0) < I_Zone\Transition[0] - (SelectedMap = "") Then
		PlayerZone = 1
	Else
		PlayerZone = 0
	EndIf
	
	TempLightVolume = 0
	
	Local FoundNewPlayerRoom% = False
	
	If PlayerRoom <> Null Then
		If Abs(EntityY(Collider) - EntityY(PlayerRoom\OBJ)) < 1.5 Then
			x = Abs(PlayerRoom\x - EntityX(Collider, True))
			If x < 4.0 Then
				z = Abs(PlayerRoom\z - EntityZ(Collider, True))
				If z < 4.0 Then
					FoundNewPlayerRoom = True
				EndIf
			EndIf
			
			If FoundNewPlayerRoom = False Then ; ~ It's likely that an adjacent room is the new player room, check for that
				For i = 0 To 3
					If PlayerRoom\Adjacent[i] <> Null Then
						x = Abs(PlayerRoom\Adjacent[i]\x - EntityX(Collider, True))
						If x < 4.0 Then
							z = Abs(PlayerRoom\Adjacent[i]\z - EntityZ(Collider, True))
							If z < 4.0 Then
								FoundNewPlayerRoom = True
								PlayerRoom = PlayerRoom\Adjacent[i]
								Exit
							EndIf
						EndIf
					EndIf
				Next
			EndIf
		Else
			FoundNewPlayerRoom = True ; ~ PlayerRoom stays the same when you're high up, or deep down
		EndIf
	EndIf
	
	For r.Rooms = Each Rooms
		x = Abs(r\x - EntityX(Collider, True))
		z = Abs(r\z - EntityZ(Collider, True))
		r\Dist = Max(x, z)
		
		If x < 16 And z < 16 Then
			For i = 0 To MaxRoomEmitters - 1
				If r\SoundEmitter[i] <> 0 Then 
					Dist = EntityDistance(r\SoundEmitterOBJ[i], Collider)
					If Dist < r\SoundEmitterRange[i] Then
						r\SoundEmitterCHN[i] = LoopSound2(RoomAmbience[r\SoundEmitter[i]], r\SoundEmitterCHN[i], Camera, r\SoundEmitterOBJ[i], r\SoundEmitterRange[i])
					EndIf
				EndIf
			Next
			
			If (Not FoundNewPlayerRoom) And (PlayerRoom <> r) Then				
				If x < 4.0 Then
					If z < 4.0 Then
						If Abs(EntityY(Collider) - EntityY(r\OBJ)) < 1.5 Then PlayerRoom = r
						FoundNewPlayerRoom = True
					EndIf
				EndIf				
			EndIf
		EndIf
		
		Hide = True
		
		If r = PlayerRoom Then Hide = False
		If Hide Then
			If IsRoomAdjacent(PlayerRoom, r) Then Hide = False
		EndIf
		If Hide Then
			For i = 0 To 3
				If IsRoomAdjacent(PlayerRoom\Adjacent[i], r) Then Hide = False : Exit
			Next
		EndIf
		
		If Hide Then
			HideEntity(r\OBJ)
		Else
			ShowEntity(r\OBJ)
			For i = 0 To MaxRoomLights - 1
				If r\Lights[i] <> 0 Then
					Dist = EntityDistance(Collider, r\Lights[i])
					If Dist < HideDistance Then
						TempLightVolume = TempLightVolume + r\LightIntensity[i]*r\LightIntensity[i] * ((HideDistance - Dist) / HideDistance)						
					EndIf
				Else
					Exit
				EndIf
			Next
			If DebugHUD
				If r\TriggerboxAmount > 0
					For i = 0 To r\TriggerboxAmount - 1
						EntityColor(r\Triggerbox[i], 255, 255, 0)
						EntityAlpha(r\Triggerbox[i], 0.2)
					Next
				EndIf
			Else
				If r\TriggerboxAmount > 0
					For i = 0 To r\TriggerboxAmount - 1
						EntityColor(r\Triggerbox[i], 255, 255, 255)
						EntityAlpha(r\Triggerbox[i], 0.0)
					Next
				EndIf
 			EndIf
		EndIf
	Next
	
	MapFound(Floor(EntityX(PlayerRoom\OBJ) / 8.0), Floor(EntityZ(PlayerRoom\OBJ) / 8.0)) = 1
	PlayerRoom\Found = True
	
	TempLightVolume = Max(TempLightVolume / 4.5, 1.0)
	
	If PlayerRoom <> Null Then
		EntityAlpha(GetChild(PlayerRoom\OBJ, 2), 1.0)
		For i = 0 To 3
			If PlayerRoom\Adjacent[i] <> Null Then
				If PlayerRoom\AdjDoor[i] <> Null
					x = Abs(EntityX(Collider, True) - EntityX(PlayerRoom\AdjDoor[i]\FrameOBJ, True))
					z = Abs(EntityZ(Collider, True) - EntityZ(PlayerRoom\AdjDoor[i]\FrameOBJ, True))
					If PlayerRoom\AdjDoor[i]\OpenState = 0 Then
						EntityAlpha(GetChild(PlayerRoom\Adjacent[i]\OBJ, 2), 0.0)
					ElseIf (Not EntityInView(PlayerRoom\AdjDoor[i]\FrameOBJ, Camera))
						EntityAlpha(GetChild(PlayerRoom\Adjacent[i]\OBJ, 2), 0.0)
					Else
						EntityAlpha(GetChild(PlayerRoom\Adjacent[i]\OBJ, 2), 1.0)
					EndIf
				EndIf
				
				For j = 0 To 3
					If (PlayerRoom\Adjacent[i]\Adjacent[j] <> Null) Then
						If (PlayerRoom\Adjacent[i]\Adjacent[j] <> PlayerRoom) Then EntityAlpha(GetChild(PlayerRoom\Adjacent[i]\Adjacent[j]\OBJ, 2), 0.0)
					EndIf
				Next
			EndIf
		Next
	EndIf
	
	CatchErrors("UpdateErrors")
End Function

Function IsRoomAdjacent(this.Rooms, that.Rooms)
	Local i%
	
	If this = Null Then Return(False)
	If this = that Then Return(True)
	For i = 0 To 3
		If that = this\Adjacent[i] Then Return(True)
	Next
	Return(False)
End Function

Global LightVolume#, TempLightVolume#

Function AddLight%(room.Rooms, x#, y#, z#, lType%, Range#, R%, G%, B%)
	Local i%
	
	If room <> Null Then
		For i = 0 To MaxRoomLights - 1
			If room\Lights[i] = 0 Then
				room\Lights[i] = CreateLight(lType)
				LightRange(room\Lights[i], Range)
				LightColor(room\Lights[i], R, G, B)
				PositionEntity(room\Lights[i], x, y, z, True)
				EntityParent(room\Lights[i], room\OBJ)
				
				room\LightIntensity[i] = (R + G + B) / 255.0 / 3.0
				
				room\LightSprites[i] = CreateSprite()
				PositionEntity(room\LightSprites[i], x, y, z)
				ScaleSprite(room\LightSprites[i], 0.13 , 0.13)
				EntityTexture(room\LightSprites[i], LightSpriteTex(0))
				EntityBlend(room\LightSprites[i], 3)
				
				EntityParent(room\LightSprites[i], room\OBJ)
				
				room\LightSpritesPivot[i] = CreatePivot()
				EntityRadius(room\LightSpritesPivot[i], 0.05)
				PositionEntity(room\LightSpritesPivot[i], x, y, z)
				EntityParent(room\LightSpritesPivot[i], room\OBJ)
				
				room\LightSprites2[i] = CreateSprite()
				PositionEntity(room\LightSprites2[i], x, y, z)
				ScaleSprite(room\LightSprites2[i], 0.6, 0.6)
				EntityTexture(room\LightSprites2[i], LightSpriteTex(2))
				EntityBlend(room\LightSprites2[i], 3)
				EntityOrder(room\LightSprites2[i], -1)
				EntityColor(room\LightSprites2[i], R, G, B)
				EntityParent(room\LightSprites2[i], room\OBJ)
				EntityFX(room\LightSprites2[i], 1)
				RotateEntity(room\LightSprites2[i], 0.0, 0.0, Rnd(360.0))
				SpriteViewMode(room\LightSprites2[i], 1)
				room\LightSpriteHidden[i] = True
				HideEntity(room\LightSprites2[i])
				room\LightFlicker[i] = Rand(1, 10)
				
				room\LightR[i] = R
				room\LightG[i] = G
				room\LightB[i] = B
				
				HideEntity(room\Lights[i])
				
				room\MaxLights = room\MaxLights + 1
				
				Return(room\Lights[i])
			EndIf
		Next
	Else
		Local Light%, Sprite%
		
		Light = CreateLight(lType)
		LightRange(Light, Range)
		LightColor(Light, R, G, B)
		PositionEntity(Light, x, y, z, True)
		Sprite = CreateSprite()
		PositionEntity(Sprite, x, y, z)
		ScaleSprite(Sprite, 0.13 , 0.13)
		EntityTexture(Sprite, LightSpriteTex(0))
		EntityBlend(Sprite, 3)
		Return(Light)
	EndIf
End Function

Type LightTemplates
	Field roomtemplate.RoomTemplates
	Field lType%
	Field x#, y#, z#
	Field Range#
	Field R%, G%, B%
	Field Pitch#, Yaw#
	Field InnerConeAngle%, OuterConeAngle#
End Type 

Function AddTempLight.LightTemplates(rt.RoomTemplates, x#, y#, z#, lType%, Range#, R%, G%, B%)
	lt.Lighttemplates = New LightTemplates
	lt\roomtemplate = rt
	lt\x = x
	lt\y = y
	lt\z = z
	lt\lType = lType
	lt\Range = Range
	lt\r = R
	lt\g = G
	lt\b = B
	
	Return(lt)
End Function

Type TempWayPoints
	Field x#, y#, z#
	Field roomtemplate.RoomTemplates
End Type 

Type WayPoints
	Field OBJ
	Field door.Doors
	Field room.Rooms
	Field State%
	Field connected.WayPoints[5]
	Field Dist#[5]
	Field Fcost#, Gcost#, Hcost#
	Field parent.WayPoints
End Type

Function CreateWaypoint.WayPoints(x#, y#, z#, door.Doors, room.Rooms)
	w.Waypoints = New WayPoints
	
	If 1 Then
		w\OBJ = CreatePivot()
		PositionEntity(w\OBJ, x, y, z)	
	Else
		w\OBJ = CreateSprite()
		PositionEntity(w\OBJ, x, y, z)
		ScaleSprite(w\OBJ, 0.15 , 0.15)
		EntityTexture(w\OBJ, LightSpriteTex(0))
		EntityBlend(w\OBJ, 3)	
	EndIf
	
	EntityParent(w\OBJ, room\OBJ)
	
	w\room = room
	w\door = door
	
	Return(w)
End Function

Function InitWayPoints(loadingstart = 45)
	Local d.Doors, w.WayPoints, w2.WayPoints, r.Rooms, ClosestRoom.Rooms
	Local x#, y#, z#
	Local i%, n%
	
	Temper = MilliSecs()
	
	Local Dist#, Dist2#
	
	For d.Doors = Each Doors
		If d\OBJ <> 0 Then HideEntity(d\OBJ)
		If d\OBJ2 <> 0 Then HideEntity(d\OBJ2)	
		If d\FrameOBJ <> 0 Then HideEntity(d\FrameOBJ)
		
		If d\room = Null Then 
			ClosestRoom.Rooms = Null
			Dist = 30.0
			For r.Rooms = Each Rooms
				x = Abs(EntityX(r\OBJ, True) - EntityX(d\FrameOBJ, True))
				If x < 20.0 Then
					z = Abs(EntityZ(r\OBJ, True) - EntityZ(d\FrameOBJ, True))
					If z < 20.0 Then
						Dist2 = x * x + z * z
						If Dist2 < Dist Then
							ClosestRoom = r
							Dist = Dist2
						EndIf
					EndIf
				EndIf
			Next
		Else
			ClosestRoom = d\room
		EndIf
		If (Not d\DisableWaypoint) Then CreateWaypoint(EntityX(d\frameOBJ, True), EntityY(d\frameOBJ, True) + 0.18, EntityZ(d\frameOBJ, True), d, ClosestRoom)
	Next
	
	Amount# = 0
	For w.WayPoints = Each WayPoints
		EntityPickMode(w\OBJ, 1, True)
		EntityRadius(w\OBJ, 0.2)
		Amount = Amount + 1
	Next
	
	Number = 0
	Iter = 0
	For w.WayPoints = Each WayPoints
		Number = Number + 1
		Iter = Iter + 1
		If Iter = 20 Then 
			DrawLoading(loadingstart + Floor((35.0 / Amount) * Number)) 
			Iter = 0
		EndIf
		
		w2.WayPoints = After(w)
		
		Local CanCreateWayPoint% = False
		
		While (w2 <> Null)
			If (w\room = w2\room Or w\door <> Null Or w2\door <> Null) Then
				Dist = EntityDistance(w\OBJ, w2\OBJ)
				
				If w\room\MaxWayPointY = 0.0 Or w2\room\MaxWayPointY = 0.0
					CanCreateWayPoint = True
				Else
					If Abs(EntityY(w\OBJ) - EntityY(w2\OBJ)) =< w\room\MaxWayPointY
						CanCreateWayPoint = True
					EndIf
				EndIf
				
				If Dist < 7.0 Then
					If CanCreateWayPoint
						If EntityVisible(w\OBJ, w2\OBJ) Then
							For i = 0 To 4
								If w\connected[i] = Null Then
									w\connected[i] = w2.WayPoints 
									w\Dist[i] = Dist
									Exit
								EndIf
							Next
							
							For n = 0 To 4
								If w2\connected[n] = Null Then 
									w2\connected[n] = w.WayPoints 
									w2\Dist[n] = Dist
									Exit
								EndIf					
							Next
						EndIf
					EndIf	
				EndIf
			EndIf
			w2 = After(w2)
		Wend
	Next
	
	For d.Doors = Each Doors
		If d\OBJ <> 0 Then ShowEntity(d\OBJ)
		If d\OBJ2 <> 0 Then ShowEntity(d\OBJ2)	
		If d\FrameOBJ <> 0 Then ShowEntity(d\FrameOBJ)		
	Next
	
	For w.WayPoints = Each WayPoints
		EntityPickMode(w\OBJ, 0, 0)
		EntityRadius(w\OBJ, 0)
		
		For i = 0 To 4
			If w\connected[i] <> Null Then 
				tLine = CreateLine(EntityX(w\OBJ, True), EntityY(w\OBJ, True), EntityZ(w\OBJ, True), EntityX(w\connected[i]\OBJ, True), EntityY(w\connected[i]\OBJ, True), EntityZ(w\connected[i]\OBJ, True))
				EntityColor(tLine, 255, 0, 0)
				EntityParent(tLine, w\OBJ)
			EndIf
		Next
	Next
End Function

Function RemoveWaypoint(w.WayPoints)
	FreeEntity(w\OBJ)
	Delete(w)
End Function

Dim MapF(MapWidth + 1, MapHeight + 1), MapG(MapWidth + 1, MapHeight + 1), MapH(MapWidth + 1, MapHeight + 1)
Dim MapState(MapWidth + 1, MapHeight + 1)
Dim MapParent(MapWidth + 1, MapHeight + 1, 2)

Function FindPath(n.NPCs, x#, y#, z#)
	Local Temp%, Dist#, Dist2#
	Local xTemp#, yTemp#, zTemp#
	Local w.WayPoints, StartPoint.WayPoints, EndPoint.WayPoints   
	Local StartX% = Floor(EntityX(n\Collider, True) / 8.0 + 0.5), StartZ% = Floor(EntityZ(n\Collider, True) / 8.0 + 0.5)
	Local EndX% = Floor(x / 8.0 + 0.5), EndZ% = Floor(z / 8.0 + 0.5)
	Local CurrX#, CurrZ#
	Local i%

   ; ~ Pathstatus = 0, route hasn't been searched for yet
   ; ~ Pathstatus = 1, route found
   ; ~ Pathstatus = 2, route not found (target unreachable)
	
	For w.WayPoints = Each WayPoints
		w\State = 0
		w\Fcost = 0
		w\Gcost = 0
		w\Hcost = 0
	Next
	
	n\PathStatus = 0
	n\PathLocation = 0
	For i = 0 To 19
		n\Path[i] = Null
	Next
	
	
	Local Pvt% = CreatePivot()
	PositionEntity(Pvt, x, y, z, True)   
	
	Temp = CreatePivot()
	PositionEntity(Temp, EntityX(n\Collider, True), EntityY(n\Collider, True) + 0.15, EntityZ(n\Collider, True))
	
	Dist = 350.0
	For w.WayPoints = Each WayPoints
		xTemp = EntityX(w\OBJ, True) - EntityX(Temp, True)
		zTemp = EntityZ(w\OBJ, True) - EntityZ(Temp, True)
		yTemp = EntityY(w\OBJ, True) - EntityY(Temp, True)
		Dist2 = (xTemp * xTemp) + (yTemp * yTemp) + (zTemp * zTemp)
		If Dist2 < Dist Then 
			; ~ Prefer waypoints that are visible
			If Not EntityVisible(w\OBJ, Temp) Then Dist2 = Dist2 * 3
			If Dist2 < Dist Then 
				Dist = Dist2
				StartPoint = w
			EndIf
		EndIf
	Next
	FreeEntity(Temp)
	
	If StartPoint = Null Then Return(2)
	StartPoint\State = 1      
	
	EndPoint = Null
	Dist = 400.0
	For w.WayPoints = Each WayPoints
		xTemp = EntityX(Pvt, True) - EntityX(w\OBJ, True)
		zTemp = EntityZ(Pvt, True) - EntityZ(w\OBJ, True)
		yTemp = EntityY(Pvt, True) - EntityY(w\OBJ, True)
		Dist2 = (xTemp * xTemp) + (yTemp * yTemp) + (zTemp * zTemp)
		
		If Dist2 < Dist Then
			Dist = Dist2
			EndPoint = w
		EndIf            
	Next
	
	FreeEntity(Pvt)
	
	If EndPoint = StartPoint Then
		If Dist < 0.4 Then
			Return(0)
		Else
			n\Path[0] = EndPoint
			Return(1)               
		EndIf
	EndIf
	If EndPoint = Null Then Return(2)
	
	Repeat
		Temp = False
		smallest.WayPoints = Null
		Dist = 10000.0
		For w.WayPoints = Each WayPoints
			If w\State = 1 Then
                Temp = True
                If (w\Fcost) < Dist Then
					Dist = w\Fcost
					smallest = w
                EndIf
			EndIf
		Next
		
		If smallest <> Null Then
			w = smallest
			w\State = 2
			For i = 0 To 4
                If w\connected[i] <> Null Then
					If w\connected[i]\State < 2 Then
						If w\connected[i]\State = 1 Then
							gTemp# = w\Gcost + w\Dist[i]
							If n\NPCtype = NPCtypeMTF Then
								If w\connected[i]\door = Null Then gTemp = gTemp + 0.5
							EndIf
							If gTemp < w\connected[i]\Gcost Then
								w\connected[i]\Gcost = gTemp
								w\connected[i]\Fcost = w\connected[i]\Gcost + w\connected[i]\Hcost
								w\connected[i]\parent = w
							EndIf
						Else
							w\connected[i]\Hcost = Abs(EntityX(w\connected[i]\OBJ, True) - EntityX(EndPoint\OBJ, True)) + Abs(EntityZ(w\connected[i]\OBJ, True) - EntityZ(EndPoint\OBJ, True))
							gTemp# = w\Gcost + w\Dist[i]
							If n\NPCtype = NPCtypeMTF Then
								If w\connected[i]\door = Null Then gTemp = gTemp + 0.5
							EndIf
							w\connected[i]\Gcost = gTemp
							w\connected[i]\Fcost = w\Gcost + w\Hcost
							w\connected[i]\parent = w
							w\connected[i]\State = 1
						EndIf            
					EndIf
					
                EndIf
			Next
			Else
			If EndPoint\State > 0 Then
                StartPoint\parent = Null
                EndPoint\State = 2
                Exit
			EndIf
		EndIf
		
		If EndPoint\State > 0 Then
			StartPoint\parent = Null
			EndPoint\State = 2
			Exit
		EndIf
	Until Temp = False
	
	If EndPoint\State > 0 Then
		Local currpoint.WayPoints = EndPoint
		Local twentiethpoint.WayPoints = EndPoint
		Local Length% = 0
		
		Repeat
			Length = Length + 1
			currpoint = currpoint\parent
			If Length > 20 Then
                twentiethpoint = twentiethpoint\parent
			EndIf
		Until currpoint = Null
		
		currpoint.WayPoints = EndPoint
		While twentiethpoint <> Null
			Length = Min(Length - 1, 19)
			twentiethpoint = twentiethpoint\parent
			n\Path[Length] = twentiethpoint
		Wend
		Return(1)
	Else
		Return(2)
	EndIf
End Function

Function CreateLine(x1#, y1#, z1#, x2#, y2#, z2#, Mesh% = 0)
	If Mesh = 0 Then 
		Mesh = CreateMesh()
		EntityFX(Mesh, 16)
		Surf = CreateSurface(Mesh)	
		Verts = 0	
		
		AddVertex(Surf, x1, y1, z1, 0, 0)
	Else
		Surf = GetSurface(Mesh, 1)
		Verts = CountVertices(Surf) - 1
	End If
	
	AddVertex(Surf, (x1 + x2) / 2.0, (y1 + y2) / 2.0, (z1 + z2) / 2.0, 0.0, 0.0) 
	; ~ You could skip creating the above vertex and change the line below to
	; ~ So your line mesh would use less vertices, the drawback is that some videocards (like the matrox g400)
	; ~ Aren't able to create a triangle with 2 vertices. so, it's your call :)
	AddVertex(Surf, x2, y2, z2, 1.0, 0.0)
	
	AddTriangle(Surf, Verts, Verts + 2, Verts + 1)
	
	Return(Mesh)
End Function

Global SelectedScreen.Screens

Type Screens
	Field OBJ%
	Field ImgPath$
	Field Img%
	Field room.Rooms
End Type

Type TempScreens
	Field ImgPath$
	Field x#, y#, z#
	Field roomtemplate.RoomTemplates
End Type

Function CreateScreen.Screens(x#, y#, z#, ImgPath$, r.Rooms)
	s.Screens = New Screens
	s\OBJ = CreatePivot()
	EntityPickMode(s\OBJ, 1)	
	EntityRadius(s\OBJ, 0.1)
	
	PositionEntity(s\OBJ, x, y, z)
	s\ImgPath = ImgPath
	s\room = r
	EntityParent(s\OBJ, r\OBJ)
	
	Return(s)
End Function

Function UpdateScreens()
	If SelectedScreen <> Null Then Return
	If SelectedDoor <> Null Then Return
	
	For s.Screens = Each Screens
		If s\room = PlayerRoom Then
			If EntityDistance(Collider, s\OBJ) < 1.2 Then
				EntityPick(Camera, 1.2)
				If PickedEntity() = s\OBJ And s\ImgPath <> "" Then
					DrawHandIcon = True
					If MouseUp1 Then 
						SelectedScreen = s
						s\Img = LoadImage_Strict("GFX\screens\" + s\ImgPath)
						s\Img = ResizeImage2(s\Img, ImageWidth(s\Img) * MenuScale, ImageHeight(s\Img) * MenuScale)
						MaskImage(s\img, 255, 0, 255)
						PlaySound_Strict(ButtonSFX)
						MouseUp1 = False
					EndIf
				EndIf
			EndIf
		EndIf
	Next
End Function

Dim MapName$(MapWidth, MapHeight)
Dim MapRoomID%(ROOM4 + 1)
Dim MapRoom$(ROOM4 + 1, 0)

Dim GorePics%(10)
Global SelectedMonitor.SecurityCams
Global CoffinCam.SecurityCams

Type SecurityCams
	Field OBJ%, MonitorOBJ%
	Field BaseOBJ%, CameraOBJ%
	Field ScrOBJ%, ScrWidth#, ScrHeight#
	Field Screen%, Cam%, ScrTexture%, ScrOverlay%
	Field Angle#, Turn#, CurrAngle#
	Field State#, PlayerState%
	Field SoundCHN%
	Field InSight%
	Field RenderInterval#
	Field room.Rooms
	Field FollowPlayer%
	Field CoffinEffect%
	Field AllowSaving%
	Field MinAngle#, MaxAngle#, Dir%
End Type

Global ScreenTexs%[2]

Global CurrRoom2slRenderCam%
Global Room2slCam%

Function CreateSecurityCam.SecurityCams(x#, y#, z#, r.Rooms, Screen% = False)
	Local sc.SecurityCams = New SecurityCams
	Local o.Objects = First Objects
	
	sc\OBJ = CopyEntity(o\CamModelID[0])
	ScaleEntity(sc\OBJ, 0.0015, 0.0015, 0.0015)
	sc\CameraOBJ = CopyEntity(o\CamModelID[1])
	ScaleEntity(sc\CameraOBJ, 0.01, 0.01, 0.01)
	
	sc\room = r
	
	sc\Screen = Screen
	If Screen Then
		sc\AllowSaving = True
		
		sc\RenderInterval = 12
		
		Local Scale# = RoomScale * 4.5 * 0.4
		
		sc\ScrOBJ = CreateSprite()
		EntityFX(sc\ScrOBJ, 17)
		SpriteViewMode(sc\ScrOBJ, 2)
		sc\ScrTexture = 0
		EntityTexture(sc\ScrOBJ, ScreenTexs[sc\ScrTexture])
		ScaleSprite(sc\ScrOBJ, MeshWidth(o\MonitorModelID[0]) * Scale * 0.95 * 0.5, MeshHeight(o\MonitorModelID[0]) * Scale * 0.95 * 0.5)
		
		sc\ScrOverlay = CreateSprite(sc\ScrOBJ)
		ScaleSprite(sc\ScrOverlay, MeshWidth(o\MonitorModelID[0]) * Scale * 0.95 * 0.5, MeshHeight(o\MonitorModelID[0]) * Scale * 0.95 * 0.5)
		MoveEntity(sc\ScrOverlay, 0.0, 0.0, -0.0005)
		EntityTexture(sc\ScrOverlay, MonitorTexture)
		SpriteViewMode(sc\ScrOverlay, 2)
		EntityBlend(sc\ScrOverlay, 3)
		
		sc\MonitorOBJ = CopyEntity(o\MonitorModelID[0], sc\ScrOBJ)
		
		ScaleEntity(sc\MonitorOBJ, Scale, Scale, Scale)
		
		sc\Cam = CreateCamera()
		CameraViewport(sc\Cam, 0, 0, 512, 512)
		CameraRange(sc\Cam, 0.05, 8.0)
		CameraZoom(sc\Cam, 0.8)
		HideEntity(sc\Cam)	
	End If
	
	PositionEntity(sc\OBJ, x, y, z)
	
	If r <> Null Then EntityParent(sc\OBJ, r\OBJ)
	
	Return(sc)
End Function

Function UpdateSecurityCams()
	CatchErrors("Uncaught (UpdateSecurityCams)")
	
	Local sc.SecurityCams
	
	; ~ CoffinEffect = 0, not affected by SCP-895
	; ~ CoffinEffect = 1, constantly affected by SCP-895
	; ~ CoffinEffect = 2, SCP-079 can broadcast SCP-895 feed on this screen
	; ~ CoffinEffect = 3, SCP-079 broadcasting SCP-895 feed
	
	For sc.SecurityCams = Each SecurityCams
		Local Close% = False
		
		If sc\room = Null Then
			HideEntity(sc\Cam)
		Else
			If sc\room\Dist < 6.0 Or PlayerRoom = sc\room Then 
				Close = True
			ElseIf sc\Cam <> 0
				HideEntity(sc\Cam)
			EndIf
			
			If sc\room <> Null
				If sc\room\RoomTemplate\Name = "room2sl" Then sc\CoffinEffect = 0
			EndIf
			
			If Close Or sc = CoffinCam Then 
				If sc\FollowPlayer Then
					If sc <> CoffinCam
						If EntityVisible(sc\CameraOBJ, Camera)
							If MTF_CameraCheckTimer > 0.0
								MTF_CameraCheckDetected = True
							EndIf
						EndIf
					EndIf
					PointEntity(sc\CameraOBJ, Camera)
					
					Local Temp# = EntityPitch(sc\CameraOBJ)
					
					RotateEntity(sc\OBJ, 0.0, CurveAngle(EntityYaw(sc\CameraOBJ), EntityYaw(sc\OBJ), 75.0), 0.0)
					
					If Temp < 40.0 Then Temp = 40.0
					If Temp > 70.0 Then Temp = 70.0
					RotateEntity(sc\CameraOBJ, CurveAngle(Temp, EntityPitch(sc\CameraOBJ), 75.0), EntityYaw(sc\OBJ), 0.0)
					
					PositionEntity(sc\CameraOBJ, EntityX(sc\OBJ, True), EntityY(sc\OBJ, True) - 0.083, EntityZ(sc\OBJ, True))
					RotateEntity(sc\CameraOBJ, EntityPitch(sc\CameraOBJ), EntityYaw(sc\OBJ), 0.0)
				Else
					If sc\Turn > 0.0 Then
						If sc\Dir = 0 Then
							sc\CurrAngle = sc\CurrAngle + 0.2 * FPSfactor
							If sc\CurrAngle > sc\Turn * 1.3 Then sc\Dir = 1
						Else
							sc\CurrAngle = sc\CurrAngle - 0.2 * FPSfactor
							If sc\CurrAngle < (-sc\Turn) * 1.3 Then sc\Dir = 0
						End If
					End If
					RotateEntity(sc\OBJ, 0.0, sc\room\Angle + sc\Angle + Max(Min(sc\CurrAngle, sc\Turn), -sc\Turn), 0.0)
					
					PositionEntity(sc\CameraOBJ, EntityX(sc\OBJ, True), EntityY(sc\OBJ, True) - 0.083, EntityZ(sc\OBJ, True))
					RotateEntity(sc\CameraOBJ, EntityPitch(sc\CameraOBJ), EntityYaw(sc\OBJ), 0.0)
					
					If sc\Cam <> 0 Then 
						PositionEntity(sc\Cam, EntityX(sc\CameraOBJ, True), EntityY(sc\CameraOBJ, True), EntityZ(sc\CameraOBJ, True))
						RotateEntity(sc\Cam, EntityPitch(sc\CameraOBJ), EntityYaw(sc\CameraOBJ), 0.0)
						MoveEntity(sc\Cam, 0.0, 0.0, 0.1)
					EndIf
					
					If sc <> CoffinCam
						If (Abs(DeltaYaw(sc\CameraOBJ, Camera)) < 60.0)
							If EntityVisible(sc\CameraOBJ, Camera)
								If MTF_CameraCheckTimer > 0.0
									MTF_CameraCheckDetected = True
								EndIf
							EndIf
						EndIf
					EndIf
				EndIf
			EndIf
			
			If Close = True Then
				If sc\Screen Then
					sc\State = sc\State + FPSfactor
					If BlinkTimer > -5.0 And EntityInView(sc\ScrOBJ, Camera) Then
						If EntityVisible(Camera, sc\ScrOBJ) Then
							If (sc\CoffinEffect = 1 Or sc\CoffinEffect = 3) And Wearing714 = 0 And WearingHazmat < 3 And WearingGasMask < 3 Then
								If BlinkTimer > -5.0
									Sanity = Sanity - FPSfactor
									RestoreSanity = False
								EndIf
							EndIf
						EndIf
					EndIf
					
					If Sanity < (-1000.0) Then 
						DeathMSG = Chr(34) + "What we know is that he died of cardiac arrest. My guess is that it was caused by SCP-895, although it has never been observed affecting video equipment from this far before. "
						DeathMSG = DeathMSG + "Further testing is needed to determine whether SCP-895's " + Chr(34) + "Red Zone" + Chr(34) + " is increasing." + Chr(34)
						
						If VomitTimer < -10.0 Then
							Kill()
						EndIf
					EndIf
					
					If VomitTimer < 0.0 And Sanity < -800.0 Then
						RestoreSanity = False
						Sanity = -1010.0
					EndIf
					
					If BlinkTimer > -5.0 And EntityInView(sc\ScrOBJ, Camera) And EntityVisible(Camera, sc\ScrOBJ) Then
						sc\InSight = True
					Else
						sc\InSight = False
					EndIf
					
					If (sc\State >= sc\RenderInterval)
						If BlinkTimer > -5.0 And EntityInView(sc\ScrOBJ, Camera)Then
							If EntityVisible(Camera, sc\ScrOBJ) Then
								If CoffinCam = Null Or Rand(5) = 5 Or sc\CoffinEffect <> 3 Then
									HideEntity(Camera)
									ShowEntity(sc\Cam)
									Cls
									
									UpdateRoomLights(sc\Cam)
									
									SetBuffer BackBuffer()
									RenderWorld()
									CopyRect(0, 0, 512, 512, 0, 0, BackBuffer(), TextureBuffer(ScreenTexs[sc\ScrTexture]))
									
									HideEntity(sc\Cam)
									ShowEntity(Camera)										
								Else
									HideEntity(Camera)
									ShowEntity(CoffinCam\room\OBJ)
									EntityAlpha(GetChild(CoffinCam\room\OBJ, 2), 1.0)
									ShowEntity(CoffinCam\Cam)
									Cls
									
									UpdateRoomLights(CoffinCam\Cam)
									
									SetBuffer BackBuffer()
									RenderWorld()
									CopyRect(0, 0, 512, 512, 0, 0, BackBuffer(), TextureBuffer(ScreenTexs[sc\ScrTexture]))
									
									HideEntity(CoffinCam\room\OBJ)
									HideEntity(CoffinCam\Cam)
									ShowEntity(Camera)										
								EndIf
							EndIf
						EndIf
						sc\State = 0.0
					EndIf
					
					If (sc\CoffinEffect = 1 Or sc\CoffinEffect = 3) And Wearing714 = 0 And WearingHazmat < 3 And WearingGasMask < 3 Then
						If sc\InSight Then
							Local Pvt% = CreatePivot()
							
							PositionEntity(Pvt, EntityX(Camera), EntityY(Camera), EntityZ(Camera))
							PointEntity(Pvt, sc\ScrOBJ)
							
							RotateEntity(Collider, EntityPitch(Collider), CurveAngle(EntityYaw(Pvt), EntityYaw(Collider), Min(Max(15000.0 / (-Sanity), 20.0), 200.0)), 0.0)
							
							TurnEntity(Pvt, 90.0, 0.0, 0.0)
							User_Camera_Pitch = CurveAngle(EntityPitch(Pvt), User_Camera_Pitch + 90.0, Min(Max(15000.0 / (-Sanity), 20.0), 200.0))
							User_Camera_Pitch = User_Camera_Pitch - 90.0
							
							FreeEntity(Pvt)
							If (sc\CoffinEffect = 1 Or sc\CoffinEffect = 3) And (Wearing714 = 0 Or WearingGasMask < 3 Or WearingHazmat < 3) Then
								If Sanity < -800 Then
									If Rand(3) = 1 Then EntityTexture(sc\ScrOverlay, MonitorTexture)
									If Rand(6) < 5 Then
										EntityTexture(sc\ScrOverlay, GorePics(Rand(0, 5)))
										If sc\PlayerState = 1 Then PlaySound_Strict(HorrorSFX(1))
										sc\PlayerState = 2
										If sc\SoundCHN = 0 Then
											sc\SoundCHN = PlaySound_Strict(HorrorSFX(4))
										Else
											If ChannelPlaying(sc\SoundCHN) = False Then sc\SoundCHN = PlaySound_Strict(HorrorSFX(4))
										End If
										If sc\CoffinEffect = 3 And Rand(200) = 1 Then sc\CoffinEffect = 2 : sc\PlayerState = Rand(10000, 20000)
									End If	
									BlurTimer = 1000.0
									If VomitTimer = 0.0 Then
										VomitTimer = 1.0
									EndIf
								ElseIf Sanity < -500.0
									If Rand(7) = 1 Then EntityTexture(sc\ScrOverlay, MonitorTexture)
									If Rand(50) = 1 Then
										EntityTexture(sc\ScrOverlay, GorePics(Rand(0, 5)))
										If sc\PlayerState = 0 Then PlaySound_Strict(HorrorSFX(0))
										sc\PlayerState = Max(sc\PlayerState, 1)
										If sc\CoffinEffect = 3 And Rand(100) = 1 Then sc\CoffinEffect = 2 : sc\PlayerState = Rand(10000, 20000)
									End If
								Else
									EntityTexture(sc\ScrOverlay, MonitorTexture)
								EndIf
							EndIf
						EndIf
					Else
						If sc\InSight Then
							If Wearing714 = 1 Or WearingHazmat = 3 Or WearingGasMask = 3 Then
								EntityTexture(sc\ScrOverlay, MonitorTexture)
							EndIf
						EndIf
					EndIf
					
					If sc\InSight And sc\CoffinEffect = 0 Or sc\CoffinEffect = 2 Then
						If sc\PlayerState = 0 Then
							sc\PlayerState = Rand(60000, 65000)
						EndIf
						
						If Rand(500) = 1 Then
							EntityTexture(sc\ScrOverlay, OldAiPics(0))
						End If
						
						If (MilliSecs2() Mod sc\PlayerState) >= Rand(600) Then
							EntityTexture(sc\ScrOverlay, MonitorTexture)
						Else
							If sc\SoundCHN = 0 Then
								sc\SoundCHN = PlaySound_Strict(LoadTempSound("SFX\SCP\079\Broadcast" + Rand(1, 3) + ".ogg"))
								If sc\CoffinEffect = 2 Then sc\CoffinEffect = 3 : sc\PlayerState = 0
							ElseIf (Not ChannelPlaying(sc\SoundCHN))
								sc\SoundCHN = PlaySound_Strict(LoadTempSound("SFX\SCP\079\Broadcast" + Rand(1, 3) + ".ogg"))
								If sc\CoffinEffect = 2 Then sc\CoffinEffect = 3 : sc\PlayerState = 0
							EndIf
							EntityTexture(sc\ScrOverlay, OldAiPics(0))
						EndIf
					EndIf
				EndIf
				If (Not sc\InSight) Then sc\SoundCHN = LoopSound2(CameraSFX, sc\SoundCHN, Camera, sc\CameraOBJ, 4.0)
			EndIf
			
			If sc <> Null Then
				If sc\room <> Null Then
					CatchErrors("UpdateSecurityCameras (" + sc\room\RoomTemplate\Name + ")")
				Else
					CatchErrors("UpdateSecurityCameras (screen has no room)")
				EndIf
			Else
				CatchErrors("UpdateSecurityCameras (screen doesn't exist anymore)")
			EndIf
		EndIf
	Next
	Cls
End Function

Function UpdateMonitorSaving()
	Local sc.SecurityCams
	Local Close% = False
	
	If SelectedDifficulty\saveType <> SAVEONSCREENS Then Return
	
	For sc = Each SecurityCams
		If sc\AllowSaving And sc\Screen Then
			Close = False
			If sc\room\Dist < 6.0 Or PlayerRoom = sc\room Then 
				Close = True
			EndIf
			
			If Close And GrabbedEntity = 0 And ClosestButton = 0 Then
				If EntityInView(sc\ScrOBJ, Camera) And EntityDistance(sc\ScrOBJ, Camera) < 1.0 Then
					If EntityVisible(sc\ScrOBJ, Camera) Then
						DrawHandIcon = True
						If MouseHit1 Then SelectedMonitor = sc
					Else
						If SelectedMonitor = sc Then SelectedMonitor = Null
					EndIf
				Else
					If SelectedMonitor = sc Then SelectedMonitor = Null
				EndIf
				
				If SelectedMonitor = sc Then
					If sc\InSight Then
						Local Pvt% = CreatePivot()
						
						PositionEntity(Pvt, EntityX(Camera), EntityY(Camera), EntityZ(Camera))
						PointEntity(Pvt, sc\ScrOBJ)
						RotateEntity(Collider, EntityPitch(Collider), CurveAngle(EntityYaw(Pvt), EntityYaw(Collider), Min(Max(15000.0 / (-Sanity), 20.0), 200.0)), 0.0)
						TurnEntity(Pvt, 90.0, 0.0, 0.0)
						User_Camera_Pitch = CurveAngle(EntityPitch(Pvt), User_Camera_Pitch + 90.0, Min(Max(15000.0 / (-Sanity), 20.0), 200.0))
						User_Camera_Pitch = User_Camera_Pitch - 90.0
						FreeEntity(Pvt)
					EndIf
				EndIf
			Else
				If SelectedMonitor = sc Then SelectedMonitor = Null
			EndIf
		EndIf
	Next
End Function

Function UpdateLever(OBJ%, Locked% = False)
	Local Dist# = EntityDistance(Camera, OBJ)
	
	If Dist < 8.0 Then 
		If Dist < 0.8 And (Not Locked) Then 
			If EntityInView(OBJ, Camera) Then 
				EntityPick(Camera, 0.65)
				
				If PickedEntity() = OBJ Then
					DrawHandIcon = True
					If MouseHit1 Then GrabbedEntity = OBJ
				End If
				
				PrevPitch# = EntityPitch(OBJ)
				
				If MouseDown1 Or MouseHit1 Then
					If GrabbedEntity <> 0 Then
						If GrabbedEntity = OBJ Then
							DrawHandIcon = True 
							RotateEntity(GrabbedEntity, Max(Min(EntityPitch(OBJ) + Max(Min(Mouse_Y_Speed_1 * 8.0, 30.0), -30.0), 80.0), -80.0), EntityYaw(OBJ), 0.0)
							
							DrawArrowIcon(0) = True
							DrawArrowIcon(2) = True
						EndIf
					EndIf
				EndIf 
				
				If EntityPitch(OBJ, True) > 75 Then
					If PrevPitch =< 75.0 Then PlaySound2(LeverSFX, Camera, OBJ, 1.0)
				ElseIf EntityPitch(OBJ, True) < -75.0
					If PrevPitch >= -75.0 Then PlaySound2(LeverSFX, Camera, OBJ, 1.0)	
				EndIf						
			EndIf
		EndIf
		
		If MouseDown1 = False And MouseHit1 = False Then 
			If EntityPitch(OBJ, True) > 0.0 Then
				RotateEntity(OBJ, CurveValue(80.0, EntityPitch(OBJ), 10.0), EntityYaw(OBJ), 0.0)
			Else
				RotateEntity(OBJ, CurveValue(-80.0, EntityPitch(OBJ), 10.0), EntityYaw(OBJ), 0.0)
			EndIf
			GrabbedEntity = 0
		End If
	EndIf
	
	If EntityPitch(OBJ, True) > 0 Then
		Return(True)
	Else
		Return(False)
	EndIf	
End Function

Function UpdateButton(OBJ%)
	Local Dist# = EntityDistance(Collider, OBJ)
	
	If Dist < 0.8 Then
		Local Temp% = CreatePivot()
		
		PositionEntity(Temp, EntityX(Camera), EntityY(Camera), EntityZ(Camera))
		PointEntity(Temp, OBJ)
		
		If EntityPick(Temp, 0.65) = OBJ Then
			If ClosestButton = 0 Then 
				ClosestButton = OBJ
			Else
				If Dist < EntityDistance(Collider, ClosestButton) Then ClosestButton = OBJ
			End If							
		End If
		FreeEntity(Temp)
	EndIf			
End Function

Function UpdateElevators#(State#, door1.Doors, door2.Doors, room1, room2, event.Events, IgnoreRotation% = True)
	Local x#, z#, Sound%
	Local Dist#, Dir#, n.NPCs, it.Items
	
	door1\IsElevatorDoor = 1
	door2\IsElevatorDoor = 1
	If door1\Open = True And door2\Open = False And door1\OpenState = 180.0 Then 
		State = -1.0
		door1\Locked = False
		If (ClosestButton = door2\Buttons[0] Or ClosestButton = door2\Buttons[1]) And MouseHit1 Then
			UseDoor(door1, False)
		EndIf
	ElseIf door2\Open = True And door1\open = False And door2\OpenState = 180.0 Then
		State = 1.0
		door2\Locked = False
		If (ClosestButton = door1\Buttons[0] Or ClosestButton = door1\Buttons[1]) And MouseHit1 Then
			UseDoor(door2, False)
		EndIf
	ElseIf Abs(door1\OpenState - door2\OpenState) < 0.2 Then
		door1\IsElevatorDoor = 2
		door2\IsElevatorDoor = 2
	EndIf
	
	door1\Locked = True
	door2\Locked = True
	If door1\Open Then
		door1\IsElevatorDoor = 3
		If Abs(EntityX(Collider) - EntityX(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
			If Abs(EntityZ(Collider) - EntityZ(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then	
				If Abs(EntityY(Collider) - EntityY(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then	
					door1\Locked = False
					door1\IsElevatorDoor = 1
				EndIf
			EndIf
		EndIf
	EndIf
	If door2\Open Then
		door2\IsElevatorDoor = 3
		If Abs(EntityX(Collider) - EntityX(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
			If Abs(EntityZ(Collider) - EntityZ(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then	
				If Abs(EntityY(Collider) - EntityY(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
					door2\Locked = False
					door2\IsElevatorDoor = 1
				EndIf
			EndIf
		EndIf	
	EndIf
	
	Local Inside% = False
	
	If door1\Open = False And door2\Open = False Then
		door1\Locked = True
		door2\Locked = True
		If door1\OpenState = 0.0 And door2\openstate = 0.0 Then
			If State < 0.0 Then
				State = State - FPSfactor
				If Abs(EntityX(Collider) - EntityX(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
					If Abs(EntityZ(Collider) - EntityZ(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then	
						If Abs(EntityY(Collider) - EntityY(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then	
							Inside = True
							
							If event\SoundCHN = 0 Then
								event\SoundCHN = PlaySound_Strict(ElevatorMoveSFX)
							Else
								If (Not ChannelPlaying(event\SoundCHN)) Then event\SoundCHN = PlaySound_Strict(ElevatorMoveSFX)
							EndIf
							
							CameraShake = Sin(Abs(State) / 3.0) * 0.3
						EndIf
					EndIf
				EndIf
				
				If State < -500.0 Then
					door1\Locked = True
					door2\Locked = False
					State = 0.0
					
					If Inside Then
						If (Not IgnoreRotation) Then
							Dist = Distance(EntityX(Collider, True), EntityZ(Collider, True), EntityX(room1, True), EntityZ(room1, True))
							Dir = Point_Direction(EntityX(Collider, True), EntityZ(Collider, True), EntityX(room1, True), EntityZ(room1, True))
							Dir = Dir + EntityYaw(room2, True) - EntityYaw(room1, True)
							Dir = WrapAngle(Dir)
							x = Max(Min(Cos(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
							z = Max(Min(Sin(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
							RotateEntity(Collider, EntityPitch(Collider, True), EntityYaw(room2, True) + AngleDist(EntityYaw(Collider, True), EntityYaw(room1, True)), EntityRoll(Collider, True), True)
						Else
							x = Max(Min((EntityX(Collider) - EntityX(room1, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
							z = Max(Min((EntityZ(Collider) - EntityZ(room1, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
						EndIf
						
						TeleportEntity(Collider, EntityX(room2, True) + x, (0.1 * FPSfactor) + EntityY(room2, True) + (EntityY(Collider) - EntityY(room1, True)), EntityZ(room2, True) + z, 0.3, True)
						UpdateDoorsTimer = 0.0
						DropSpeed = 0.0
						UpdateDoors()
						UpdateRooms()
						
						Sound = Rand(0, 2)
						door2\SoundCHN = PlaySound_Strict(OpenDoorSFX(3, Sound))
					EndIf
					
					For n.NPCs = Each NPCs
						If Abs(EntityX(n\Collider) - EntityX(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
							If Abs(EntityZ(n\Collider) - EntityZ(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
								If Abs(EntityY(n\Collider) - EntityY(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
									If (Not IgnoreRotation) Then
										Dist = Distance(EntityX(n\Collider, True), EntityZ(n\Collider, True), EntityX(room1, True), EntityZ(room1, True))
										Dir = Point_Direction(EntityX(n\Collider, True), EntityZ(n\Collider, True), EntityX(room1, True), EntityZ(room1, True))
										Dir = Dir + EntityYaw(room2, True) - EntityYaw(room1, True)
										Dir = WrapAngle(Dir)
										x = Max(Min(Cos(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min(Sin(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										RotateEntity(n\Collider, EntityPitch(n\Collider, True), EntityYaw(room2, True) + AngleDist(EntityYaw(n\Collider, True), EntityYaw(room1, True)), EntityRoll(n\Collider, True), True)
									Else
										x = Max(Min((EntityX(n\Collider) - EntityX(room1, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min((EntityZ(n\Collider) - EntityZ(room1, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
									EndIf
									
									TeleportEntity(n\Collider, EntityX(room2, True) + x, (0.1 * FPSfactor) + EntityY(room2, True) + (EntityY(n\Collider) - EntityY(room1, True)), EntityZ(room2, True) + z, n\CollRadius, True)
									If n = Curr173
										Curr173\IdleTimer = 10
									EndIf
								EndIf
							EndIf
						EndIf
					Next
					
					For it.Items = Each Items
						If Abs(EntityX(it\Collider) - EntityX(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
							If Abs(EntityZ(it\Collider) - EntityZ(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
								If Abs(EntityY(it\Collider) - EntityY(room1, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
									If (Not IgnoreRotation) Then
										Dist = Distance(EntityX(it\Collider, True), EntityZ(it\Collider, True), EntityX(room1, True), EntityZ(room1, True))
										Dir = Point_Direction(EntityX(it\Collider, True), EntityZ(it\Collider, True),EntityX(room1, True), EntityZ(room1, True))
										Dir = Dir + EntityYaw(room2, True) - EntityYaw(room1, True)
										Dir = WrapAngle(Dir)
										x = Max(Min(Cos(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min(Sin(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										RotateEntity(it\Collider, EntityPitch(it\Collider, True), EntityYaw(room2, True) + AngleDist(EntityYaw(it\Collider, True), EntityYaw(room1, True)), EntityRoll(it\Collider, True), True)
									Else
										x = Max(Min((EntityX(it\Collider) - EntityX(room1, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min((EntityZ(it\Collider) - EntityZ(room1, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
									EndIf
									TeleportEntity(it\Collider, EntityX(room2, True) + x, (0.1 * FPSfactor) + EntityY(room2, True) + (EntityY(it\Collider) - EntityY(room1, True)), EntityZ(room2, True) + z, 0.01, True)
								EndIf
							EndIf
						EndIf
					Next
					UseDoor(door2, False, Not Inside)
					door1\open = False
					
					PlaySound2(ElevatorBeepSFX, Camera, room1, 4.0)
				EndIf
			Else
				State = State + FPSfactor
				If Abs(EntityX(Collider) - EntityX(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
					If Abs(EntityZ(Collider) - EntityZ(room2, True)) <  280.0 * RoomScale + (0.015 * FPSfactor) Then	
						If Abs(EntityY(Collider) - EntityY(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
							Inside = True
							
							If event\SoundCHN = 0 Then
								event\SoundCHN = PlaySound_Strict(ElevatorMoveSFX)
							Else
								If (Not ChannelPlaying(event\SoundCHN)) Then event\SoundCHN = PlaySound_Strict(ElevatorMoveSFX)
							EndIf
							
							CameraShake = Sin(Abs(State) / 3.0) * 0.3
						EndIf
					EndIf
				EndIf	
				
				If State > 500.0 Then 
					door1\Locked = False
					door2\Locked = True				
					State = 0.0
					
					If Inside Then	
						If (Not IgnoreRotation) Then
							Dist = Distance(EntityX(Collider, True), EntityZ(Collider, True), EntityX(room2, True), EntityZ(room2, True))
							Dir = Point_Direction(EntityX(Collider, True), EntityZ(Collider, True), EntityX(room2, True), EntityZ(room2, True))
							Dir = Dir + EntityYaw(room1, True) - EntityYaw(room2, True)
							x = Max(Min(Cos(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
							z = Max(Min(Sin(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
							RotateEntity(Collider, EntityPitch(Collider, True), EntityYaw(room2, True) + AngleDist(EntityYaw(Collider, True), EntityYaw(room1, True)), EntityRoll(Collider, True), True)
						Else
							x = Max(Min((EntityX(Collider) - EntityX(room2, True)), 280 * RoomScale - 0.22), (-280) * RoomScale + 0.22)
							z = Max(Min((EntityZ(Collider) - EntityZ(room2, True)), 280 * RoomScale - 0.22), (-280) * RoomScale + 0.22)
						EndIf
						TeleportEntity(Collider, EntityX(room1, True) + x, (0.1 * FPSfactor) + EntityY(room1, True) + (EntityY(Collider) - EntityY(room2, True)), EntityZ(room1, True) + z, 0.3, True)
						UpdateDoorsTimer = 0.0
						DropSpeed = 0.0
						UpdateDoors()
						UpdateRooms()
						
						Sound = Rand(0, 2)
						door1\SoundCHN = PlaySound_Strict(OpenDoorSFX(3, Sound))
					EndIf
					
					For n.NPCs = Each NPCs
						If Abs(EntityX(n\Collider) - EntityX(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
							If Abs(EntityZ(n\Collider) - EntityZ(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
								If Abs(EntityY(n\Collider) - EntityY(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
									If (Not IgnoreRotation) Then
										Dist = Distance(EntityX(n\Collider, True), EntityZ(n\Collider, True), EntityX(room2, True), EntityZ(room2, True))
										Dir = Point_Direction(EntityX(n\Collider, True), EntityZ(n\Collider, True), EntityX(room2, True), EntityZ(room2, True))
										Dir = Dir + EntityYaw(room1, True) - EntityYaw(room2, True)
										x = Max(Min(Cos(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min(Sin(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										RotateEntity(n\Collider, EntityPitch(n\Collider, True), EntityYaw(room2, True) + AngleDist(EntityYaw(n\Collider, True), EntityYaw(room1, True)), EntityRoll(n\Collider, True), True)
									Else
										x = Max(Min((EntityX(n\Collider) - EntityX(room2, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min((EntityZ(n\Collider) - EntityZ(room2, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
									EndIf
									TeleportEntity(n\Collider, EntityX(room1, True) + x, (0.1 * FPSfactor) + EntityY(room1, True) + (EntityY(n\Collider) - EntityY(room2, True)), EntityZ(room1, True) + z, n\CollRadius, True)
									If n = Curr173
										Curr173\IdleTimer = 10.0
									EndIf
								EndIf
							EndIf
						EndIf
					Next
					
					For it.Items = Each Items
						If Abs(EntityX(it\Collider) - EntityX(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
							If Abs(EntityZ(it\Collider) - EntityZ(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
								If Abs(EntityY(it\Collider) - EntityY(room2, True)) < 280.0 * RoomScale + (0.015 * FPSfactor) Then
									If (Not IgnoreRotation) Then
										Dist = Distance(EntityX(it\Collider, True), EntityZ(it\Collider, True), EntityX(room2, True), EntityZ(room2, True))
										Dir = Point_Direction(EntityX(it\Collider, True), EntityZ(it\Collider, True), EntityX(room2, True), EntityZ(room2, True))
										Dir = Dir + EntityYaw(room1, True) - EntityYaw(room2, True)
										x = Max(Min(Cos(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min(Sin(Dir) * Dist, 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										RotateEntity(it\Collider, EntityPitch(it\Collider, True), EntityYaw(room2, True) + AngleDist(EntityYaw(it\Collider, True), EntityYaw(room1, True)), EntityRoll(it\Collider, True), True)
									Else
										x = Max(Min((EntityX(it\Collider) - EntityX(room2, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
										z = Max(Min((EntityZ(it\Collider) - EntityZ(room2, True)), 280.0 * RoomScale - 0.22), (-280.0) * RoomScale + 0.22)
									EndIf
									TeleportEntity(it\Collider, EntityX(room1, True) + x, (0.1 * FPSfactor) + EntityY(room1, True) + (EntityY(it\Collider) - EntityY(room2, True)), EntityZ(room1, True) + z, 0.01, True)
								EndIf
							EndIf
						EndIf
					Next
					UseDoor(door1, False, Not Inside)
					door2\open = False
					
					PlaySound2(ElevatorBeepSFX, Camera, room2, 4.0)
				EndIf	
			EndIf
		EndIf
	EndIf
	
	Return(State)
End Function

Type Props
	Field File$
	Field OBJ%
End Type

Function CreatePropObj(File$)
	Local p.Props
	
	For p.Props = Each Props
		If p\File = File Then
			Return CopyEntity(p\OBJ)
		EndIf
	Next
	
	p.Props = New Props
	p\File = File
	p\OBJ = LoadMesh(File)
	Return(p\OBJ)
End Function

Function CreateMap()
	I_Zone\Transition[0] = 13
	I_Zone\Transition[1] = 7
	I_Zone\HasCustomForest = False
	I_Zone\HasCustomMT = False
	
	Local x%, y%, Temp%
	Local i%, x2%, y2%
	Local Width%, Height%
	Local Zone%
	
	SeedRnd(GenerateSeedNumber(RandomSeed))
	
	Dim MapName$(MapWidth, MapHeight)
	
	Dim MapRoomID%(ROOM4 + 1)
	
	x = Floor(MapWidth / 2)
	y = MapHeight - 2
	
	For i = y To MapHeight - 1
		MapTemp(x, i) = True
	Next
	
	Repeat
		Width = Rand(10, 15)
		
		If x > MapWidth * 0.6 Then
			Width = -Width
		ElseIf x > MapWidth * 0.4
			x = x - Width / 2 
		EndIf
		
		; ~ Make sure the hallway doesn't go outside the array
		If x + Width > MapWidth - 3 Then
			Width = MapWidth - 3 - x
		ElseIf x + Width < 2
			Width = (-x) + 2
		EndIf
		
		x = Min(x, x + Width)
		Width = Abs(Width)
		For i = x To x + Width
			MapTemp(Min(i, MapWidth), y) = True
		Next
		
		Height = Rand(3, 4)
		If y - Height < 1 Then Height = y - 1
		
		yHallways = Rand(4, 5)
		
		If GetZone(y - Height) <> GetZone(y - Height + 1) Then Height = Height - 1
		
		For i = 1 To yHallways
			x2 = Max(Min(Rand(x, x + Width - 1), MapWidth - 2), 2)
			While MapTemp(x2, y - 1) Or MapTemp(x2 - 1, y - 1) Or MapTemp(x2 + 1, y - 1)
				x2 = x2 + 1
			Wend
			
			If x2 < x + Width Then
				If i = 1 Then
					TempHeight = Height 
					If Rand(2) = 1 Then x2 = x Else x2 = x + Width
				Else
					TempHeight = Rand(1, Height)
				EndIf
				
				For y2 = y - TempHeight To y
					If GetZone(y2) <> GetZone(y2 + 1) Then ; ~ A room leading from zone to another
						MapTemp(x2, y2) = 255
					Else
						MapTemp(x2, y2) = True
					EndIf
				Next
				
				If TempHeight = Height Then Temp = x2
			End If
		Next
		x = Temp
		y = y - Height
	Until y < 2
	
	Local ZoneAmount% = 3
	Local Room1Amount%[3], Room2Amount%[3], Room2CAmount%[3], Room3Amount%[3], Room4Amount%[3]
	
	; ~ Count the amount of rooms
	For y = 1 To MapHeight - 1
		Zone = GetZone(y)
		For x = 1 To MapWidth - 1
			If MapTemp(x, y) > 0 Then
				Temp = Min(MapTemp(x + 1, y), 1) + Min(MapTemp(x - 1, y), 1)
				Temp = Temp + Min(MapTemp(x, y + 1), 1) + Min(MapTemp(x, y - 1), 1)			
				If MapTemp(x, y) < 255 Then MapTemp(x, y) = Temp
				Select MapTemp(x, y)
					Case 1
						;[Block]
						Room1Amount[Zone] = Room1Amount[Zone] + 1
						;[End Block]
					Case 2
						;[Block]
						If Min(MapTemp(x + 1, y), 1) + Min(MapTemp(x - 1, y), 1) = 2 Then
							Room2Amount[Zone] = Room2Amount[Zone] + 1	
						ElseIf Min(MapTemp(x, y + 1), 1) + Min(MapTemp(x , y - 1), 1) = 2
							Room2Amount[Zone] = Room2Amount[Zone] + 1	
						Else
							Room2CAmount[Zone] = Room2CAmount[Zone] + 1
						EndIf
						;[End Block]
					Case 3
						;[Block]
						Room3Amount[Zone] = Room3Amount[Zone] + 1
						;[End Block[
					Case 4
						;[Block]
						Room4Amount[Zone] = Room4Amount[Zone]+1
						;[End Block]
				End Select
			EndIf
		Next
	Next		
	
	; ~ Force more room1s (if needed)
	For i = 0 To 2
		; ~ Need more rooms if there are less than 5 of them
		Temp = (-Room1Amount[i]) + 5
		
		If Temp > 0 Then
			For y = (MapHeight / ZoneAmount) * (2 - i) + 1 To ((MapHeight / ZoneAmount) * ((2 - i) + 1.0)) - 2			
				For x = 2 To MapWidth - 2
					If MapTemp(x, y) = 0 Then
						If (Min(MapTemp(x + 1, y), 1) + Min(MapTemp(x - 1, y), 1) + Min(MapTemp(x, y + 1), 1) + Min(MapTemp(x, y - 1), 1)) = 1 Then
							If MapTemp(x + 1, y) Then
								x2 = x + 1 : y2 = y
							ElseIf MapTemp(x - 1, y)
								x2 = x - 1 : y2 = y
							ElseIf MapTemp(x, y + 1)
								x2 = x : y2 = y + 1	
							ElseIf MapTemp(x, y - 1)
								x2 = x : y2 = y - 1
							EndIf
							
							Placed = False
							If MapTemp(x2, y2) > 1 And MapTemp(x2, y2) < 4 Then 
								Select MapTemp(x2, y2)
									Case 2
										;[Block]
										If Min(MapTemp(x2 + 1, y2), 1) + Min(MapTemp(x2 - 1, y2), 1) = 2 Then
											Room2Amount[i] = Room2Amount[i] - 1
											Room3Amount[i] = Room3Amount[i] + 1
											Placed = True
										ElseIf Min(MapTemp(x2, y2 + 1), 1) + Min(MapTemp(x2, y2 - 1), 1) = 2
											Room2Amount[i] = Room2Amount[i] - 1
											Room3Amount[i] = Room3Amount[i] + 1
											Placed = True
										EndIf
										;[End Block]
									Case 3
										;[Block]
										Room3Amount[i] = Room3Amount[i] - 1
										Room4Amount[i] = Room4Amount[i] + 1	
										placed = True
										;[End Block]
								End Select
								
								If Placed Then
									MapTemp(x2, y2) = MapTemp(x2, y2) + 1
									
									MapTemp(x, y) = 1
									Room1Amount[i] = Room1Amount[i] + 1	
									
									Temp = Temp - 1
								EndIf
							EndIf
						EndIf
					EndIf
					If Temp = 0 Then Exit
				Next
				If Temp = 0 Then Exit
			Next
		EndIf
	Next
	
	; ~ Force more room4s and room2Cs
	For i = 0 To 2
		Select i
			Case 2
				;[Block]
				Zone = 2
				Temp2 = MapHeight / 3
				;[End Block]
			Case 1
				;[Block]
				Zone = MapHeight / 3 + 1
				Temp2 = MapHeight * (2.0 / 3.0) - 1
				;[End Block]
			Case 0
				;[Block]
				Zone = MapHeight * (2.0 / 3.0) + 1
				Temp2 = MapHeight - 2
				;[End Block]
		End Select
		
		If Room4Amount[i] < 1 Then ; ~ We want at least 1 ROOM4
			Temp = 0
			For y = Zone To Temp2
				For x = 2 To MapWidth - 2
					If MapTemp(x, y) = 3 Then
						Select 0 ; ~ See if adding a ROOM1 is possible
							Case (MapTemp(x + 1, y) Or MapTemp(x + 1, y + 1) Or MapTemp(x + 1, y - 1) Or MapTemp(x + 2, y))
								;[Block]
								MapTemp(x + 1, y) = 1
								Temp = 1
								;[End Block]
							Case (MapTemp(x - 1, y) Or MapTemp(x - 1, y + 1) Or MapTemp(x - 1, y - 1) Or MapTemp(x - 2, y))
								;[Block]
								MapTemp(x - 1, y) = 1
								Temp = 1
								;[End Block]
							Case (MapTemp(x, y + 1) Or MapTemp(x + 1, y + 1) Or MapTemp(x - 1, y + 1) Or MapTemp(x, y + 2))
								;[Block]
								MapTemp(x, y + 1) = 1
								Temp = 1
								;[End Block]
							Case (MapTemp(x, y - 1) Or MapTemp(x + 1, y - 1) Or MapTemp(x - 1, y - 1) Or MapTemp(x, y - 2))
								;[Block]
								MapTemp(x, y - 1) = 1
								Temp = 1
								;[End Block]
						End Select
						If Temp = 1 Then
							MapTemp(x, y) = 4 ; ~ Turn this room into a ROOM4
							Room4Amount[i] = Room4Amount[i] + 1
							Room3Amount[i] = Room3Amount[i] - 1
							Room1Amount[i] = Room1Amount[i] + 1
						EndIf
					EndIf
					If Temp = 1 Then Exit
				Next
				If Temp = 1 Then Exit
			Next
		EndIf
		
		If Room2CAmount[i] < 1 Then ; ~ We want at least 1 ROOM2C
			Temp = 0
			
			Zone = Zone + 1
			Temp2 = Temp2 - 1
			
			For y = Zone To Temp2
				For x = 3 To MapWidth - 3
					If MapTemp(x, y) = 1 Then
						Select True ; ~ See if adding some rooms is possible
							Case MapTemp(x - 1, y) > 0
								;[Block]
								If (MapTemp(x, y - 1) + MapTemp(x, y + 1) + MapTemp(x + 2, y)) = 0 Then
									If (MapTemp(x + 1, y - 2) + MapTemp(x + 2, y - 1) + MapTemp(x + 1, y - 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x + 1, y) = 2
										MapTemp(x + 1, y - 1) = 1
										Temp = 1
									Else If (MapTemp(x + 1, y + 2) + MapTemp(x + 2, y + 1) + MapTemp(x + 1, y + 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x + 1, y) = 2
										MapTemp(x + 1, y + 1) = 1
										Temp = 1
									EndIf
								EndIf
								;[End Block]
							Case MapTemp(x + 1, y) > 0
								;[Block]
								If (MapTemp(x, y - 1) + MapTemp(x, y + 1) + MapTemp(x - 2, y)) = 0 Then
									If (MapTemp(x - 1, y - 2) + MapTemp(x - 2, y - 1) + MapTemp(x - 1, y - 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x - 1, y) = 2
										MapTemp(x - 1, y - 1) = 1
										Temp = 1
									Else If (MapTemp(x - 1, y + 2) + MapTemp(x - 2, y + 1) + MapTemp(x - 1, y + 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x - 1, y) = 2
										MapTemp(x - 1, y + 1 ) = 1
										Temp = 1
									EndIf
								EndIf
								;[End Block]
							Case MapTemp(x, y - 1) > 0
								;[Block]
								If (MapTemp(x - 1, y) + MapTemp(x + 1, y) + MapTemp(x, y + 2)) = 0 Then
									If (MapTemp(x - 2, y + 1) + MapTemp(x - 1, y + 2) + MapTemp(x - 1, y + 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x, y + 1) = 2
										MapTemp(x - 1, y + 1) = 1
										Temp = 1
									Else If (MapTemp(x + 2, y + 1) + MapTemp(x + 1, y + 2) + MapTemp(x + 1, y + 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x, y + 1) = 2
										MapTemp(x + 1, y + 1) = 1
										Temp = 1
									EndIf
								EndIf
								;[End Block]
							Case MapTemp(x, y + 1) > 0
								;[Block]
								If (MapTemp(x - 1, y) + MapTemp(x + 1, y) + MapTemp(x, y - 2)) = 0 Then
									If (MapTemp(x - 2, y - 1) + MapTemp(x - 1, y - 2) + MapTemp(x - 1, y - 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x, y - 1) = 2
										MapTemp(x - 1, y - 1) = 1
										Temp = 1
									Else If (MapTemp(x + 2, y - 1) + MapTemp(x + 1, y - 2) + MapTemp(x + 1, y - 1)) = 0 Then
										MapTemp(x, y) = 2
										MapTemp(x, y - 1) = 2
										MapTemp(x + 1, y - 1) = 1
										Temp = 1
									EndIf
								EndIf
								;[End Block]
						End Select
						If Temp = 1 Then
							Room2CAmount[i] = Room2CAmount[i] + 1
							Room2Amount[i] = Room2Amount[i] + 1
						EndIf
					EndIf
					If Temp = 1 Then Exit
				Next
				If Temp = 1 Then Exit
			Next
		EndIf
	Next
	
	Local MaxRooms% = 55 * MapWidth / 20
	
	MaxRooms = Max(MaxRooms, Room1Amount[0] + Room1Amount[1] + Room1Amount[2] + 1)
	MaxRooms = Max(MaxRooms, Room2Amount[0] + Room2Amount[1] + Room2Amount[2] + 1)
	MaxRooms = Max(MaxRooms, Room2CAmount[0] + Room2CAmount[1] + Room2CAmount[2] + 1)
	MaxRooms = Max(MaxRooms, Room3Amount[0] + Room3Amount[1] + Room3Amount[2] + 1)
	MaxRooms = Max(MaxRooms, Room4Amount[0] + Room4Amount[1] + Room4Amount[2] + 1)
	
	Dim MapRoom$(ROOM4 + 1, MaxRooms)
	
	; ~ LIGHT CONTAINMENT ZONE
	
	Local Min_Pos% = 1, Max_Pos% = Room1Amount[0] - 1
	
	MapRoom(ROOM1, 0) = "start"	
	SetRoom("room372", ROOM1, Floor(0.1 * Float(Room1Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room914", ROOM1, Floor(0.3 * Float(Room1Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room1archive", ROOM1, Floor(0.5 * Float(Room1Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room205", ROOM1, Floor(0.6 * Float(Room1Amount[0])), Min_Pos, Max_Pos)
	
	MapRoom(ROOM2C, 0) = "room2clockroom"
	
	Min_Pos = 1
	Max_Pos = Room2Amount[0] - 1
	
	MapRoom(ROOM2, 0) = "room2closets" 
	SetRoom("room2testroom2", ROOM2, Floor(0.1 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room2scps", ROOM2, Floor(0.2 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room2storage", ROOM2, Floor(0.3 * Float(Room2Amount[0])),Min_Pos, Max_Pos)
	SetRoom("room2gw_b", ROOM2, Floor(0.4 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room2sl", ROOM2, Floor(0.5 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room012", ROOM2, Floor(0.55 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room2scps2", ROOM2, Floor(0.6 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room1123", ROOM2, Floor(0.7 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	SetRoom("room2elevator", ROOM2, Floor(0.85 * Float(Room2Amount[0])), Min_Pos, Max_Pos)
	
	MapRoom(ROOM2C, Floor(0.5 * Float(Room2CAmount[0]))) = "room1162"
	
	MapRoom(ROOM3, Floor(Rnd(0.2, 0.8) * Float(Room3Amount[0]))) = "room3storage"
	
	MapRoom(ROOM4, Floor(0.3 * Float(Room4Amount[0]))) = "room4info"
	
	; ~ HEAVY CONTAINMENT ZONE
	
	Min_Pos = Room1Amount[0]
	Max_Pos = Room1Amount[0] + Room1Amount[1] - 1
	
	SetRoom("room079", ROOM1, Room1Amount[0] + Floor(0.15 * Float(Room1Amount[1])), Min_Pos, Max_Pos)
    SetRoom("room106", ROOM1, Room1Amount[0] + Floor(0.3 * Float(Room1Amount[1])), Min_Pos, Max_Pos)
    SetRoom("008", ROOM1, Room1Amount[0] + Floor(0.4 * Float(Room1Amount[1])), Min_Pos, Max_Pos)
    SetRoom("room035", ROOM1, Room1Amount[0] + Floor(0.5 * Float(Room1Amount[1])), Min_Pos, Max_Pos)
    SetRoom("room895", ROOM1, Room1Amount[0] + Floor(0.7 * Float(Room1Amount[1])), Min_Pos, Max_Pos)
	
	Min_Pos = Room2Amount[0]
	Max_Pos = Room2Amount[0] + Room2Amount[1] - 1
	
	MapRoom(ROOM2, Room2Amount[0] + Floor(0.1 * Float(Room2Amount[1]))) = "room2nuke"
	SetRoom("room2tunnel", ROOM2, Room2Amount[0] + Floor(0.25 * Float(Room2Amount[1])), Min_Pos, Max_Pos)
	SetRoom("room049", ROOM2, Room2Amount[0] + Floor(0.4 * Float(Room2Amount[1])), Min_Pos, Max_Pos)
	SetRoom("room2shaft",ROOM2,Room2Amount[0] + Floor(0.6 * Float(Room2Amount[1])), Min_Pos, Max_Pos)
	SetRoom("room2testroom", ROOM2, Room2Amount[0] + Floor(0.7 * Float(Room2Amount[1])), Min_Pos, Max_Pos)
	SetRoom("room2servers", ROOM2, Room2Amount[0] + Floor(0.9 * Room2Amount[1]), Min_Pos, Max_Pos)
	
	MapRoom(ROOM2C, Room2CAmount[0] + Floor(0.5 * Float(Room2CAmount[1]))) = "room2cpit"
	
	MapRoom(ROOM3, Room3Amount[0] + Floor(0.3 * Float(Room3Amount[1]))) = "room513"
	MapRoom(ROOM3, Room3Amount[0] + Floor(0.6 * Float(Room3Amount[1]))) = "room966"
	
	; ~ ENTRANCE ZONE
	
	MapRoom(ROOM1, Room1Amount[0] + Room1Amount[1] + Room1Amount[2] - 2) = "exit1"
	MapRoom(ROOM1, Room1Amount[0] + Room1Amount[1] + Room1Amount[2] - 1) = "gateaentrance"
	MapRoom(ROOM1, Room1Amount[0] + Room1Amount[1]) = "room1lifts"
	
	Min_Pos = Room2Amount[0] + Room2Amount[1]
	Max_Pos = Room2Amount[0] + Room2Amount[1] + Room2Amount[2] - 1		
	
	MapRoom(ROOM2, Min_Pos + Floor(0.1 * Float(Room2Amount[2]))) = "room2poffices"
	SetRoom("room2cafeteria", ROOM2, Min_Pos + Floor(0.2 * Float(Room2Amount[2])), Min_Pos, Max_Pos)
	SetRoom("room2sroom", ROOM2, Min_Pos + Floor(0.3 * Float(Room2Amount[2])), Min_Pos, Max_Pos)
	SetRoom("room2servers2", ROOM2, Min_Pos + Floor(0.4 * Room2Amount[2]), Min_Pos, Max_Pos)	
	SetRoom("room2offices", ROOM2, Min_Pos + Floor(0.45 * Room2Amount[2]), Min_Pos, Max_Pos)
	SetRoom("room2offices4", ROOM2, Min_Pos + Floor(0.5 * Room2Amount[2]), Min_Pos, Max_Pos)	
	SetRoom("room860", ROOM2, Min_Pos + Floor(0.6 * Room2Amount[2]), Min_Pos, Max_Pos)
	SetRoom("medibay", ROOM2, Min_Pos + Floor(0.7 * Float(Room2Amount[2])), Min_Pos, Max_Pos)
	SetRoom("room2poffices2", ROOM2, Min_Pos + Floor(0.8*Room2Amount[2]), Min_Pos, Max_Pos)
	SetRoom("room2offices2", ROOM2, Min_Pos + Floor(0.9*Float(Room2Amount[2])), Min_Pos, Max_Pos)
	
	MapRoom(ROOM2C, Room2CAmount[0] + Room2CAmount[1]) = "room2ccont"	
	MapRoom(ROOM2C, Room2CAmount[0] + Room2CAmount[1] + 1) = "room2clockroom3"		
	
	MapRoom(ROOM3, Room3Amount[0] + Room3Amount[1] + Floor(0.3 * Float(Room3Amount[2]))) = "room3servers"
	MapRoom(ROOM3, Room3Amount[0] + Room3Amount[1] + Floor(0.7 * Float(Room3Amount[2]))) = "room3servers2"
	MapRoom(ROOM3, Room3Amount[0] + Room3Amount[1] + Floor(0.5 * Float(Room3Amount[2]))) = "room3offices"
	
	; ~ Generate the map
	Temp = 0
	
	Local r.Rooms, Spacing# = 8.0
	
	For y = MapHeight - 1 To 1 Step - 1
		If y < MapHeight / 3 + 1 Then
			Zone = 3
		ElseIf y < MapHeight * (2.0 / 3.0)
			Zone = 2
		Else
			Zone = 1
		EndIf
		
		For x = 1 To MapWidth - 2
			If MapTemp(x, y) = 255 Then
				If y > MapHeight / 2 Then
					r = CreateRoom(Zone, ROOM2, x * 8.0, 0.0, y * 8.0, "checkpoint1")
				Else
					r = CreateRoom(Zone, ROOM2, x * 8.0, 0.0, y * 8.0, "checkpoint2")
				EndIf
			ElseIf MapTemp(x, y) > 0				
				Temp = Min(MapTemp(x + 1, y), 1) + Min(MapTemp(x - 1, y), 1) + Min(MapTemp(x, y + 1), 1) + Min(MapTemp(x, y - 1), 1)
				Select Temp
					Case 1 ; ~ Generate ROOM1s
						;[Block]
						If MapRoomID(ROOM1) < MaxRooms And MapName(x, y) = "" Then
							If MapRoom(ROOM1, MapRoomID(ROOM1)) <> "" Then MapName(x, y) = MapRoom(ROOM1, MapRoomID(ROOM1))	
						EndIf
						
						r = CreateRoom(Zone, ROOM1, x * 8.0, 0.0, y * 8.0, MapName(x, y))
						If MapTemp(x, y + 1) Then
							r\Angle = 180 
							TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
						ElseIf MapTemp(x - 1, y)
							r\Angle = 270
							TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
						ElseIf MapTemp(x + 1, y)
							r\Angle = 90
							TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
						Else 
							r\Angle = 0
						End If
						MapRoomID(ROOM1) = MapRoomID(ROOM1) + 1
						;[End Block]
					Case 2 ; ~ Generate ROOM2s
						;[Block]
						If MapTemp(x - 1, y) > 0 And MapTemp(x + 1, y) > 0 Then
							If MapRoomID(ROOM2) < MaxRooms And MapName(x, y) = ""  Then
								If MapRoom(ROOM2, MapRoomID(ROOM2)) <> "" Then MapName(x, y) = MapRoom(ROOM2, MapRoomID(ROOM2))	
							EndIf
							r = CreateRoom(Zone, ROOM2, x * 8.0, 0.0, y * 8.0, MapName(x, y))
							If Rand(2) = 1 Then r\Angle = 90 Else r\Angle = 270
							TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
							MapRoomID(ROOM2) = MapRoomID(ROOM2) + 1
						ElseIf MapTemp(x, y - 1) > 0 And MapTemp(x, y + 1) > 0
							If MapRoomID(ROOM2) < MaxRooms And MapName(x, y) = ""  Then
								If MapRoom(ROOM2, MapRoomID(ROOM2)) <> "" Then MapName(x, y) = MapRoom(ROOM2, MapRoomID(ROOM2))	
							EndIf
							r = CreateRoom(Zone, ROOM2, x * 8.0, 0.0, y * 8.0, MapName(x, y))
							If Rand(2) = 1 Then r\Angle = 180 Else r\Angle = 0
							TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
							MapRoomID(ROOM2) = MapRoomID(ROOM2) + 1
						Else
							If MapRoomID(ROOM2C) < MaxRooms And MapName(x, y) = ""  Then
								If MapRoom(ROOM2C, MapRoomID(ROOM2C)) <> "" Then MapName(x, y) = MapRoom(ROOM2C, MapRoomID(ROOM2C))	
							EndIf
							
							If MapTemp(x - 1, y) > 0 And MapTemp(x, y + 1) > 0 Then
								r = CreateRoom(Zone, ROOM2C, x * 8.0, 0.0, y * 8.0, MapName(x, y))
								r\Angle = 180
								TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
							ElseIf MapTemp(x + 1, y) > 0 And MapTemp(x, y + 1) > 0
								r = CreateRoom(Zone, ROOM2C, x * 8.0, 0.0, y * 8.0, MapName(x, y))
								r\Angle = 90
								TurnEntity(r\OBJ, 0.0, r\Angle, 0.0)
							ElseIf MapTemp(x - 1, y) > 0 And MapTemp(x, y - 1) > 0
								r = CreateRoom(Zone, ROOM2C, x * 8.0, 0.0, y * 8.0, MapName(x, y))
								TurnEntity(r\OBJ, 0.0, 270.0, 0.0)
								r\Angle = 270
							Else
								r = CreateRoom(Zone, ROOM2C, x * 8.0, 0.0, y * 8.0, MapName(x, y))
							EndIf
							MapRoomID(ROOM2C) = MapRoomID(ROOM2C) + 1
						EndIf
						;[End Block]
					Case 3 ; ~ Generate ROOM3s
						;[Block]
						If MapRoomID(ROOM3) < MaxRooms And MapName(x, y) = ""  Then
							If MapRoom(ROOM3, MapRoomID(ROOM3)) <> "" Then MapName(x, y) = MapRoom(ROOM3, MapRoomID(ROOM3))	
						EndIf
						
						r = CreateRoom(Zone, ROOM3, x * 8.0, 0.0, y * 8.0, MapName(x, y))
						If (Not MapTemp(x, y - 1)) Then
							TurnEntity(r\OBJ, 0.0, 180.0, 0.0)
							r\Angle = 180
						ElseIf (Not MapTemp(x - 1, y))
							TurnEntity(r\OBJ, 0.0, 90.0, 0.0)
							r\Angle = 90
						ElseIf (Not MapTemp(x + 1, y))
							TurnEntity(r\OBJ, 0.0, -90.0, 0.0)
							r\Angle = 270
						End If
						MapRoomID(ROOM3) = MapRoomID(ROOM3) + 1
						;[End Block]
					Case 4 ; ~ Generate ROOM4s
						If MapRoomID(ROOM4) < MaxRooms And MapName(x, y) = ""  Then
							If MapRoom(ROOM4, MapRoomID(ROOM4)) <> "" Then MapName(x, y) = MapRoom(ROOM4, MapRoomID(ROOM4))	
						EndIf
						
						r = CreateRoom(Zone, ROOM4, x * 8.0, 0.0, y * 8.0, MapName(x, y))
						MapRoomID(ROOM4) = MapRoomID(ROOM4) + 1
						;[End Block]
				End Select
			EndIf
		Next
	Next		
	
	; ~ Rooms out of map
	r = CreateRoom(0, ROOM1, (MapWidth - 1) * 8.0, 500.0, 8.0, "gatea")
	MapRoomID(ROOM1) = MapRoomID(ROOM1) + 1
	
	r = CreateRoom(0, ROOM1, (MapWidth - 1) * 8.0, 0.0, (MapHeight - 1) * 8.0, "pocketdimension")
	MapRoomID(ROOM1) = MapRoomID(ROOM1) + 1	
	
	If IntroEnabled
		r = CreateRoom(0, ROOM1, 8.0, 0.0, (MapHeight - 1) * 8.0, "173")
		MapRoomID(ROOM1) = MapRoomID(ROOM1) + 1
	EndIf
	
	r = CreateRoom(0, ROOM1, 8.0, 800.0, 0.0, "dimension1499")
	MapRoomID(ROOM1) = MapRoomID(ROOM1) + 1
	
	For r.Rooms = Each Rooms
		PreventRoomOverlap(r)
	Next
	
	If 0 Then 
		Repeat
			Cls
			For x = 0 To MapWidth - 1
				For y = 0 To MapHeight - 1
					If MapTemp(x, y) = 0 Then
						Zone = GetZone(y)
						
						Color(50 * Zone, 50 * Zone, 50 * Zone)
						Rect(x * 32.0, y * 32.0, 30.0, 30.0)
					Else
						If MapTemp(x, y) = 255 Then
							Color(0, 200, 0)
						Else If MapTemp(x, y) = 4 Then
							Color(50, 50, 255)
						Else If MapTemp(x, y) = 3 Then
							Color(50, 255, 255)
						Else If MapTemp(x, y) = 2 Then
							Color(255, 255, 50)
						Else
							Color(255, 255, 255)
						EndIf
						Rect(x * 32, y * 32, 30, 30)
					End If
				Next
			Next	
			
			For x = 0 To MapWidth - 1
				For y = 0 To MapHeight - 1
					If MouseX() > x * 32 And MouseX() < x * 32 + 32 Then
						If MouseY() > y * 32 And MouseY() < y * 32 + 32 Then
							Color(255, 0, 0)
						Else
							Color(200, 200, 200)
						EndIf
					Else
						Color(200, 200, 200)
					EndIf
					
					If MapTemp(x, y) > 0 Then
						Text(x * 32 + 2, (y) * 32.0 + 2.0, MapTemp(x, y) + " " + MapName(x, y))
					End If
				Next
			Next			
			Flip
		Until KeyHit(28)		
	EndIf
	
	For y = 0 To MapHeight
		For x = 0 To MapWidth
			MapTemp(x, y) = Min(MapTemp(x, y), 1)
		Next
	Next
	
	Local d.Doors
	Local ShouldSpawnDoor%
	
	For y = MapHeight To 0 Step -1
		If y < I_Zone\Transition[1] - 1 Then
			Zone = 3
		ElseIf y >= I_Zone\Transition[1] - 1 And y < I_Zone\Transition[0] - 1 Then
			Zone = 2
		Else
			Zone = 1
		EndIf
		
		For x = MapWidth To 0 Step -1
			If MapTemp(x, y) > 0 Then
				If Zone = 2 Then Temp = 2 Else Temp = 0
                For r.Rooms = Each Rooms
					r\Angle = WrapAngle(r\Angle)
					If Int(r\x / 8.0) = x And Int(r\z / 8.0) = y Then
						ShouldSpawnDoor = False
						Select r\RoomTemplate\Shape
							Case ROOM1
								;[Block]
								If r\Angle = 90
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Case ROOM2
								;[Block]
								If r\Angle = 90 Or r\Angle = 270
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Case ROOM2C
								;[Block]
								If r\Angle = 0 Or r\Angle = 90
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Case ROOM3
								;[Block]
								If r\Angle = 0 Or r\Angle = 180 Or r\Angle = 90
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Default
								;[Block]
								ShouldSpawnDoor = True
								;[End Block]
						End Select
						
						If ShouldSpawnDoor
							If (x + 1) < (MapWidth + 1)
								If MapTemp(x + 1, y) > 0 Then
									d.Doors = CreateDoor(r\Zone, Float(x) * Spacing + Spacing / 2.0, 0, Float(y) * Spacing, 90.0, r, Max(Rand(-3, 1), 0), Temp)
									r\AdjDoor[0] = d
								EndIf
							EndIf
						EndIf
						
						ShouldSpawnDoor = False
						Select r\RoomTemplate\Shape
							Case ROOM1
								;[Block]
								If r\Angle = 180
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Case ROOM2
								;[Block]
								If r\Angle = 0 Or r\Angle = 180
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Case ROOM2C
								;[Block]
								If r\Angle = 180 Or r\Angle = 90
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Case ROOM3
								;[Block]
								If r\Angle = 180 Or r\Angle = 90 Or r\Angle = 270
									ShouldSpawnDoor = True
								EndIf
								;[End Block]
							Default
								;[Block]
								ShouldSpawnDoor = True
								;[End Block]
						End Select
						If ShouldSpawnDoor
							If (y + 1) < (MapHeight + 1)
								If MapTemp(x, y + 1) > 0 Then
									d.Doors = CreateDoor(r\Zone, Float(x) * Spacing, 0.0, Float(y) * Spacing + Spacing / 2.0, 0.0, r, Max(Rand(-3, 1), 0), Temp)
									r\AdjDoor[3] = d
								EndIf
							EndIf
						EndIf
						
						Exit
					EndIf
                Next
   			End If
		Next
	Next
	
	For r.Rooms = Each Rooms
		r\Angle = WrapAngle(r\Angle)
		r\Adjacent[0] = Null
		r\Adjacent[1] = Null
		r\Adjacent[2] = Null
		r\Adjacent[3] = Null
		For r2.Rooms = Each Rooms
			If r <> r2 Then
				If r2\z = r\z Then
					If (r2\x) = (r\x + 8.0) Then
						r\Adjacent[0] = r2
						If r\AdjDoor[0] = Null Then r\AdjDoor[0] = r2\AdjDoor[2]
					ElseIf (r2\x) = (r\x - 8.0)
						r\Adjacent[2] = r2
						If r\AdjDoor[2] = Null Then r\AdjDoor[2] = r2\AdjDoor[0]
					EndIf
				ElseIf r2\x = r\x Then
					If (r2\z) = (r\z - 8.0) Then
						r\Adjacent[1] = r2
						If r\AdjDoor[1] = Null Then r\AdjDoor[1] = r2\AdjDoor[3]
					ElseIf (r2\z) = (r\z + 8.0)
						r\Adjacent[3] = r2
						If r\AdjDoor[3] = Null Then r\AdjDoor[3] = r2\AdjDoor[1]
					EndIf
				EndIf
			EndIf
			If (r\Adjacent[0] <> Null) And (r\Adjacent[1] <> Null) And (r\Adjacent[2] <> Null) And (r\Adjacent[3] <> Null) Then Exit
		Next
	Next
End Function

Function SetRoom(Room_Name$, Room_Type%, Pos%, Min_Pos%, Max_Pos%) ; ~ Place a room without overwriting others
	Local Looped%, Can_Place%
	
	Looped = False
	Can_Place = True
	While MapRoom(Room_Type, Pos) <> ""
		Pos = Pos + 1
		If Pos > Max_Pos Then
			If Looped = False Then
				Pos = Min_Pos + 1 : Looped = True
			Else
				Can_Place = False
				Exit
			EndIf
		EndIf
	Wend
	If Can_Place = True Then
		MapRoom(Room_Type, Pos) = Room_Name
		Return(True)
	Else
		Return(False)
	EndIf
End Function

Function GetZone(y%)
	Return(Min(Floor((Float(MapWidth - y) / MapWidth * ZONEAMOUNT)), ZONEAMOUNT - 1))
End Function

Function LoadTerrain(HeightMap, yScale# = 0.7, t1%, t2%, Mask%)
	; ~ Load the HeightMap
	If HeightMap = 0 Then RuntimeError("HeightMap image " + HeightMap + " does not exist.")
	
	; ~ Store HeightMap dimensions
	Local x% = ImageWidth(HeightMap) - 1, y = ImageHeight(HeightMap) - 1
	Local lx%, ly%, Index%
	
	; ~ Load texture and lightmaps
	If t1 = 0 Then RuntimeError("Invalid texture 1")
	If t2 = 0 Then RuntimeError("Invalid texture 2")
	If Mask = 0 Then RuntimeError("Invalid texture mask")
	
	; ~ Auto scale the textures to the right size
	If t1 Then ScaleTexture(t1, x / 4, y / 4)
	If t2 Then ScaleTexture(t2, x / 4, y / 4)
	If Mask Then ScaleTexture(Mask, x, y)
	
	; ~ Start building the terrain
	Local Mesh% = CreateMesh()
	Local Surf% = CreateSurface(Mesh)
	
	; ~ Create some verts for the terrain
	For ly = 0 To y
		For lx = 0 To x
			AddVertex(Surf, lx, 0, ly, 1.0 / lx, 1.0 / ly)
		Next
	Next
	RenderWorld
			
	; ~ Connect the verts with faces
	For ly = 0 To y - 1
		For lx = 0 To x - 1
			AddTriangle(Surf, lx + ((x + 1) * ly), lx + ((x + 1) * ly) + (x + 1), (lx + 1) + ((x + 1) * ly))
			AddTriangle(Surf, (lx + 1) + ((x + 1) * ly), lx + ((x + 1) * ly) + (x + 1), (lx + 1) + ((x + 1) * ly) + (x + 1))
		Next
	Next
			
	; ~ Position the terrain to center 0, 0, 0
	Local Mesh2% = CopyMesh(Mesh, Mesh)
	Local Surf2% = GetSurface(Mesh2, 1)
	
	PositionMesh(Mesh, (-x) / 2.0, 0.0, (-y) / 2.0)
	PositionMesh(Mesh2, (-x) / 2.0, 0.01, (-y) / 2.0)
	
	; ~ Alter vertice height to match the heightmap red channel
	LockBuffer(ImageBuffer(HeightMap))
	LockBuffer(TextureBuffer(Mask))
	
	For lx = 0 To x
		For ly = 0 To y
			; ~ Using vertex alpha and two meshes instead of FE_ALPHAWHATEVER
			; ~ It doesn't look perfect but it does the job
			; ~ You might get better results by downscaling the mask to the same size as the heightmap
			Local MaskX# = Min(lx * Float(TextureWidth(Mask)) / Float(ImageWidth(HeightMap)), TextureWidth(Mask) - 1)
			Local MaskY# = TextureHeight(Mask) - Min(ly * Float(TextureHeight(Mask)) / Float(ImageHeight(HeightMap)), TextureHeight(Mask) - 1)
			
			RGB1 = ReadPixelFast(Min(lx, x - 1.0), y - Min(ly, y - 1.0), ImageBuffer(HeightMap))
			r = (RGB1 And $FF0000) Shr 16 ; ~ Separate out the red
			
			Local Alpha# = (((ReadPixelFast(Max(MaskX -5.0, 5.0), Max(MaskY - 5.0, 5.0), TextureBuffer(Mask)) And $FF000000) Shr 24) / $FF)
			
			Alpha = Alpha + (((ReadPixelFast(Min(MaskX + 5.0, TextureWidth(Mask) - 5.0), Min(MaskY + 5.0, TextureHeight(Mask) - 5), TextureBuffer(Mask)) And $FF000000) Shr 24) / $FF)
			Alpha = Alpha + (((ReadPixelFast(Max(MaskX - 5.0, 5.0), Min(MaskY + 5.0, TextureHeight(Mask) - 5.0), TextureBuffer(Mask)) And $FF000000) Shr 24) / $FF)
			Alpha = Alpha + (((ReadPixelFast(Min(MaskX + 5.0, TextureWidth(Mask) - 5.0), Max(MaskY - 5.0, 5.0), TextureBuffer(Mask)) And $FF000000) Shr 24) / $FF)
			Alpha = Alpha * 0.25
			Alpha = Sqr(Alpha)
			
			Index = lx + ((x + 1) * ly)
			VertexCoords(Surf, Index , VertexX(Surf,Index), r * yScale, VertexZ(Surf, Index))
			VertexCoords(Surf2, Index , VertexX(Surf2,Index), r * yScale, VertexZ(Surf2, Index))
			VertexColor(Surf2, Index, 255.0, 255.0, 255.0, Alpha)
			; ~ Set the terrain texture coordinates
			VertexTexCoords(Surf, Index, lx, -ly )
			VertexTexCoords(Surf2, Index, lx, -ly) 
		Next
	Next
	UnlockBuffer(TextureBuffer(Mask))
	UnlockBuffer(ImageBuffer(HeightMap))
	
	UpdateNormals(Mesh)
	UpdateNormals(Mesh2)
	
	EntityTexture(Mesh, t1, 0, 0)
	EntityTexture(Mesh2, t2, 0, 0)
	
	EntityFX(Mesh, 1)
	EntityFX(Mesh2, 1 + 2 + 32)
	
	Return(Mesh)
End Function

Include "Source Code\Skybox.bb"

Global UpdateRoomLightsTimer# = 0.0

Function UpdateRoomLights(Cam%)
	Local r.Rooms, i%, Random#, Alpha#, Dist#
	
	For r.Rooms = Each Rooms
		If r\Dist < HideDistance * 0.7 Or r = PlayerRoom Then
			For i = 0 To r\MaxLights
				If r\Lights[i] <> 0 Then
					If EnableRoomLights% And (SecondaryLightOn > 0.5) And Cam = Camera Then
						EntityOrder(r\LightSprites2[i], -1)
						If UpdateRoomLightsTimer = 0.0 Then
							ShowEntity(r\LightSprites[i])
							
							If EntityDistance(Cam, r\Lights[i]) < 8.5 Then
								If r\LightHidden[i] Then
									ShowEntity(r\Lights[i])
									r\LightHidden[i] = False
								EndIf
							Else
								If (Not r\LightHidden[i]) Then
									HideEntity(r\Lights[i])
									r\LightHidden[i] = True
								EndIf
							EndIf
							
							If EntityDistance(Cam, r\LightSprites2[i]) < 8.5 Then
								If EntityVisible(Cam, r\LightSpritesPivot[i]) Then
									If r\LightSpriteHidden[i] Then
										ShowEntity(r\LightSprites2[i])
										r\LightSpriteHidden[i] = False
									EndIf
									If PlayerRoom\RoomTemplate\Name = "173" Then
										Random = Rnd(0.38, 0.42)
									Else
										If r\LightFlicker[i] < 5 Then
											Random = Rnd(0.38, 0.42)
										ElseIf r\LightFlicker%[i] > 4 And r\LightFlicker[i] < 10 Then
											Random = Rnd(0.35, 0.45)
										Else
											Random = Rnd(0.3, 0.5)
										EndIf
									EndIf
									ScaleSprite(r\LightSprites2[i], Random, Random)
									
									Dist = (EntityDistance(Cam, r\LightSpritesPivot[i]) + 0.5) / 7.5
									Dist = Max(Min(Dist, 1.0), 0.0)
									Alpha = Float(Inverse(Dist))
									
									If Alpha > 0.0 Then
										EntityAlpha(r\LightSprites2[i], Max(3.0 * (Brightness / 255.0) * (r\LightIntensity[i] / 2), 1.0) * Alpha)
									Else
										; ~ Instead of rendering the sprite invisible, just hiding it if the player is far away from it
										If (Not r\LightSpriteHidden[i]) Then
											HideEntity(r\LightSprites2[i])
											r\LightSpriteHidden[i] = True
										EndIf
									EndIf
									
									;;;;;;;;;;;;;;
									If r\RoomTemplate\UseLightSpark Then
										If EntityDistance(Cam, r\LightSprites2[i]) >= 8.5 Or (Not EntityVisible(Cam, r\LightSpritesPivot[i])) Then
											HideEntity(r\LightSprites2[i])
											r\LightSpriteHidden[i] = True
										EndIf
									EndIf
								Else
									If (Not r\LightSpriteHidden[i]) Then
										HideEntity(r\LightSprites2[i])
										r\LightSpriteHidden[i] = True
									EndIf
								EndIf
							Else
								If (Not r\LightSpriteHidden[i]) Then
									HideEntity(r\LightSprites2[i])
									r\LightSpriteHidden[i] = True
									If r\RoomTemplate\UseLightSpark Then
										If r\LightSpark[i] <> 0 Then HideEntity(r\LightSpark[i])
									EndIf
								EndIf
							EndIf
							
							If r\RoomTemplate\UseLightSpark Then
								If r\LightSpark[i] <> 0 Then
									If r\LightSparkTimer[i] > 0.0 And r\LightSparkTimer[i] < 10.0
										ShowEntity(r\LightSpark[i])
										r\LightSparkTimer[i] = r\LightSparkTimer[i] + FPSfactor
									Else
										HideEntity(r\LightSpark[i])
										r\LightSparkTimer[i] = 0.0
									EndIf
								EndIf
							EndIf
							
							If r\RoomTemplate\UseLightSpark Then
								If r\LightFlicker[i] > 4.0 Then
									If Rand(400) = 1 Then
										SetEmitter(r\LightSpritesPivot[i], ParticleEffect[0])
										PlaySound2(IntroSFX(Rand(8, 10)), Cam, r\LightSpritesPivot[i])
										ShowEntity(r\LightSpark[i])
										r\LightSparkTimer[i] = FPSfactor
									EndIf
								EndIf
							EndIf
							Else
								If EntityDistance(Cam, r\LightSprites2[i]) < 8.5 Then
								If PlayerRoom\RoomTemplate\Name = "173" Then
									Random = Rnd(0.38, 0.42)
								Else
									If r\LightFlicker[i] < 5 Then
										Random = Rnd(0.38, 0.42)
									ElseIf r\LightFlicker[i] > 4 And r\LightFlicker[i] < 10 Then
										Random = Rnd(0.35, 0.45)
									Else
										Random = Rnd(0.3, 0.5)
									EndIf
								EndIf
								
								If (Not r\LightSpriteHidden[i]) Then
									ScaleSprite(r\LightSprites2[i], Random, Random)
								EndIf
							EndIf
							
							If r\RoomTemplate\UseLightSpark Then
								If r\LightSpark[i] <> 0 Then
									If r\LightSparkTimer[i] > 0.0 And r\LightSparkTimer[i] < 10.0 Then
										ShowEntity(r\LightSpark[i])
										r\LightSparkTimer[i] = r\LightSparkTimer[i] + FPSfactor
									Else
										HideEntity(r\LightSpark[i])
										r\LightSparkTimer[i] = 0.0
									EndIf
								EndIf
							EndIf
						EndIf
						UpdateRoomLightsTimer = UpdateRoomLightsTimer + FPSfactor
						If UpdateRoomLightsTimer >= 8.0 Then
							UpdateRoomLightsTimer = 0.0
						EndIf
					ElseIf Cam = Camera Then
						If SecondaryLightOn =< 0.5 Then
							HideEntity(r\LightSprites[i])
						Else
							ShowEntity(r\LightSprites[i])
						EndIf
						
						If (Not r\LightHidden[i]) Then
							HideEntity(r\Lights[i])
							r\LightHidden[i] = True
						EndIf
						If (Not r\LightSpriteHidden[i]) Then
							HideEntity(r\LightSprites2[i])
							r\LightSpriteHidden[i] = True
						EndIf
						If r\RoomTemplate\UseLightSpark Then
							If r\LightSpark[i] <> 0 Then HideEntity(r\LightSpark[i])
						EndIf
					Else
						; ~ This will make the lightsprites not glitch through the wall when they are rendered by the cameras
						EntityOrder(r\LightSprites2[i], 0)
					EndIf
				EndIf
			Next
		EndIf
	Next
End Function

Function UpdateCheckpointMonitors(Number%)
	Local i%, SF%, b%, t1%
	Local Entity%
	Local o.Objects = First Objects
	
	If Number = 0
		Entity = o\MonitorModelID[1]
		UpdateCheckpoint1 = True
	Else
		Entity = o\MonitorModelID[2]
		UpdateCheckpoint2 = True
	EndIf
	
	For i = 2 To CountSurfaces(Entity)
		SF = GetSurface(Entity, i)
		b = GetSurfaceBrush(SF)
		If b <> 0 Then
			t1 = GetBrushTexture(b,0)
			If t1 <> 0 Then
				Name$ = StripPath(TextureName(t1))
				If Lower(Name) <> "monitortexture.jpg"
					If Number = 0
						If MonitorTimer < 50
							BrushTexture(b, MonitorTexture2, 0, 0)
						Else
							BrushTexture(b, MonitorTexture4, 0, 0)
						EndIf
					Else
						If MonitorTimer2 < 50
							BrushTexture(b, MonitorTexture2, 0, 0)
						Else
							BrushTexture(b, MonitorTexture3, 0, 0)
						EndIf
					EndIf
					PaintSurface(SF, b)
				EndIf
				If Name <> "" Then FreeTexture(t1)
			EndIf
			FreeBrush(b)
		EndIf
	Next
End Function

Function TurnCheckpointMonitorsOff(Number%)
	Local i%, SF%, b%, t1%
	Local Entity%
	Local o.Objects = First Objects
	
	If Number = 0
		Entity = o\MonitorModelID[1]
		UpdateCheckpoint1 = False
		MonitorTimer = 0.0
	Else
		Entity = o\MonitorModelID[2]
		UpdateCheckpoint2 = False
		MonitorTimer2 = 0.0
	EndIf
	
	For i = 2 To CountSurfaces(Entity)
		SF = GetSurface(Entity, i)
		b = GetSurfaceBrush(SF)
		If b <> 0 Then
			t1 = GetBrushTexture(b, 0)
			If t1 <> 0 Then
				Name$ = StripPath(TextureName(t1))
				If Lower(Name) <> "monitortexture.jpg"
					BrushTexture(b, MonitorTextureOff, 0, 0)
					PaintSurface(SF, b)
				EndIf
				If Name <> "" Then FreeTexture(t1)
			EndIf
			FreeBrush(b)
		EndIf
	Next
End Function

Function TimeCheckpointMonitors()
	If UpdateCheckpoint1
		If MonitorTimer < 100.0
			MonitorTimer = Min(MonitorTimer + FPSfactor, 100.0)
		Else
			MonitorTimer = 0.0
		EndIf
	EndIf
	If UpdateCheckpoint2
		If MonitorTimer2 < 100.0
			MonitorTimer2 = Min(MonitorTimer2 + FPSfactor, 100.0)
		Else
			MonitorTimer2 = 0.0
		EndIf
	EndIf
End Function

Function AmbientLightRooms(Value% = 0)
	If Value = AmbientLightRoomVal Then Return
	AmbientLightRoomVal = Value
	
	Local OldBuffer% = BackBuffer() ; ~ Probably shouldn't make assumptions here but who cares, why wouldn't it use the Backbuffer(GetBuffer())
	
	SetBuffer(TextureBuffer(AmbientLightRoomTex))
	
	ClsColor(Value, Value, Value)
	Cls
	ClsColor(0, 0, 0)
	
	SetBuffer(OldBuffer)
End Function

Dim CHUNKDATA(64, 64)

Function SetChunkDataValues()
	Local StrTemp$, i%, j%
	
	StrTemp = ""
	SeedRnd GenerateSeedNumber(RandomSeed)
	
	For i = 0 To 63
		For j = 0 To 63
			CHUNKDATA(i, j) = Rand(0, GetINIInt("Data\1499chunks.ini", "general", "count"))
		Next
	Next
	
	SeedRnd(MilliSecs2())
End Function

Type ChunkPart
	Field Amount%
	Field OBJ%[128]
	Field RandomYaw#[128]
	Field ID%
End Type

Function CreateChunkParts(r.Rooms)
	Local File$ = "Data\1499chunks.ini"
	Local ChunkAmount% = GetINIInt(File, "general", "count")
	Local i%, StrTemp$, j%
	Local chp.ChunkPart, chp2.ChunkPart
	
	StrTemp = ""
	SeedRnd GenerateSeedNumber(RandomSeed)
	
	For i = 0 To ChunkAmount
		Local Loc% = GetINISectionLocation(File, "chunk" + i)
		
		If Loc > 0 Then
			StrTemp = GetINIString2(File, Loc, "count")
			chp = New ChunkPart
			chp\Amount = Int(StrTemp)
			For j = 0 To Int(StrTemp)
				Local OBJ_ID% = GetINIString2(File, Loc, "obj" + j)
				Local x$ = GetINIString2(File, Loc, "obj" + j +" -x")
				Local z$ = GetINIString2(File, Loc, "obj" + j + "-z")
				Local Yaw$ = GetINIString2(File, Loc, "obj" + j + "-yaw")
				
				chp\OBJ[j] = CopyEntity(r\Objects[OBJ_ID])
				If Lower(Yaw) = "random"
					chp\RandomYaw[j] = Rnd(360)
					RotateEntity(chp\OBJ[j], 0.0, chp\RandomYaw[j], 0.0)
				Else
					RotateEntity(chp\OBJ[j], 0.0, Float(Yaw), 0.0)
				EndIf
				PositionEntity(chp\OBJ[j], Float(x), 0, Float(z))
				ScaleEntity(chp\OBJ[j], RoomScale, RoomScale, RoomScale)
				EntityType(chp\OBJ[j], HIT_MAP)
				EntityPickMode(chp\OBJ[j], 2)
				HideEntity(chp\OBJ[j])
			Next
			chp2 = Before(chp)
			If chp2 <> Null
				chp\ID = chp2\ID + 1
			EndIf
		EndIf
	Next
	
	SeedRnd(MilliSecs2())
End Function

Type Chunk
	Field OBJ%[128]
	Field x#, z#, y#
	Field Amount%
	Field IsSpawnChunk%
	Field ChunkPivot%
	Field PlatForm%
End Type

Function CreateChunk.Chunk(OBJ%, x#, y#, z#, IsSpawnChunk% = False)
	Local ch.Chunk = New Chunk
	Local i%, chp.ChunkPart
	
	ch\ChunkPivot = CreatePivot()
	ch\x = x
	ch\y = y
	ch\z = z
	PositionEntity(ch\ChunkPivot, ch\x + 20.0, ch\y, ch\z + 20.0, True)
	
	ch\IsSpawnChunk = IsSpawnChunk
	
	If OBJ > -1
		ch\Amount = GetINIInt("Data\1499chunks.ini", "chunk" + OBJ, "count")
		For chp = Each ChunkPart
			If chp\ID = OBJ
				For i = 0 To ch\Amount
					ch\OBJ[i] = CopyEntity(chp\OBJ[i], ch\ChunkPivot)
				Next
			EndIf
		Next
	EndIf
	
	ch\PlatForm = CopyEntity(PlayerRoom\Objects[0], ch\ChunkPivot)
	EntityType(ch\PlatForm, HIT_MAP)
	EntityPickMode(ch\PlatForm, 2)
	
	Return(ch)
End Function

Function UpdateChunks(r.Rooms, ChunkPartAmount%, SpawnNPCs% = True)
	Local ch.Chunk, StrTemp$, i%, x#, z#, ch2.Chunk, y#, n.NPCs, j%
	Local ChunkX#, ChunkZ#, ChunkMaxDistance# = 3.0 * 40.0
	
	ChunkX = Int(EntityX(Collider) / 40)
	ChunkZ = Int(EntityZ(Collider) / 40)
	
	y = EntityY(PlayerRoom\OBJ)
	x = (-ChunkMaxDistance) + (ChunkX * 40.0)
	z = (-ChunkMaxDistance) + (ChunkZ * 40.0)
	
	Local CurrChunkData% = 0, MaxChunks% = GetINIInt("Data\1499chunks.ini", "general", "count")
	
	Repeat
		Local ChunkFound% = False
		
		For ch = Each Chunk
			If ch\x = x
				If ch\z = z
					ChunkFound = True
					Exit
				EndIf
			EndIf
		Next
		If (Not ChunkFound)
			CurrChunkData = CHUNKDATA(Abs(((x + 32) / 40) Mod 64), Abs(((z + 32) / 40) Mod 64))
			ch2 = CreateChunk(CurrChunkData, x, y, z)
			ch2\IsSpawnChunk = False
		EndIf
		x = x + 40.0
		If x > ChunkMaxDistance + (ChunkX * 40.0)
			z = z + 40.0
			x = (-ChunkMaxDistance) + (ChunkX * 40.0)
		EndIf
	Until z > ChunkMaxDistance + (ChunkZ * 40.0)
	
	For ch = Each Chunk
		If (Not ch\IsSpawnChunk)
			If Distance(EntityX(Collider), EntityZ(Collider), EntityX(ch\ChunkPivot), EntityZ(ch\ChunkPivot)) > ChunkMaxDistance
				FreeEntity(ch\ChunkPivot)
				Delete(ch)
			EndIf
		EndIf
	Next
	
	Local CurrNPCNumber% = 0
	
	For n = Each NPCs
		If n\NPCtype = NPCtype1499
			CurrNPCNumber = CurrNPCNumber + 1
		EndIf
	Next
	
	Local MaxNPCs% = 64 ; ~ The maximum amount of NPCs in dimension1499
	Local e.Events
	
	For e.Events = Each Events
		If e\room = PlayerRoom Then
			If e\room\NPC[0] <> Null Then
				MaxNPCs = 16
				Exit
			EndIf
		EndIf
	Next
	
	If CurrNPCNumber < MaxNPCs
		Select Rand(1, 8)
			Case 1
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(40.0, 80.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(40.0, 80.0))
				;[End Block]
			Case 2
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(40.0, 80.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(-40.0, 40.0))
				;[End Block]
			Case 3
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(40.0, 80.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(-40.0, -80.0))
				;[End Block]
			Case 4
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(-40.0, 40.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(-40.0, -80.0))
				;[End Block]
			Case 5
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(-40.0, -80.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(-40.0, -80.0))
				;[End Block]
			Case 6
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(-40.0, -80.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(-40.0, 40.0))
				;[End Block]
			Case 7
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(-40.0, -80.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(40.0, 80.0))
				;[End Block]
			Case 8
				;[Block]
				n.NPCs = CreateNPC(NPCtype1499, EntityX(Collider) + Rnd(-40.0, 40.0), EntityY(PlayerRoom\OBJ) + 0.5, EntityZ(Collider) + Rnd(40.0, 80.0))
				;[End Block]
		End Select
		If Rand(2) = 1 Then n\State2 = 500.0 * 3.0
		n\Angle = Rnd(360.0)
	Else
		For n = Each NPCs
			If n\NPCtype = NPCtype1499 Then
				If n\PrevState = 0 Then
					If EntityDistance(n\Collider, Collider) > ChunkMaxDistance Or EntityY(n\Collider) < EntityY(PlayerRoom\obj) - 5 Then
						; ~ This will be updated like this so that new NPCs can spawn for the player
						RemoveNPC(n)
					EndIf
				EndIf
			EndIf
		Next
	EndIf
	
End Function

Function HideChunks()
	Local ch.Chunk, i%
	
	For ch = Each Chunk
		If (Not ch\IsSpawnChunk)
			For i = 0 To ch\Amount
				FreeEntity(ch\OBJ[i])
			Next
			FreeEntity(ch\PlatForm)
			FreeEntity(ch\ChunkPivot)
			Delete(ch)
		EndIf
	Next
End Function

Function DeleteChunks()
	Delete Each Chunk
	Delete Each ChunkPart
End Function

Type Dummy1499
	Field Anim%
	Field OBJ%
End Type

Function UpdateLightSpark(room.Rooms)
	Local i%
	
	For i = 0 To MaxRoomLights - 1
		If room\Lights[i] <> 0 Then
			If room\LightFlicker[i] > 4.0
				room\LightSpark[i] = CreateSprite()
				ScaleSprite(room\LightSpark[i], 1.0, 1.0)
				EntityTexture(room\LightSpark[i], ParticleTextures(8))
				SpriteViewMode(room\LightSpark[i], 2)
				EntityFX(room\LightSpark[i], 1)
				RotateEntity(room\LightSpark[i], -90.0, 0.0, 0.0)
				EntityBlend(room\LightSpark[i], 3)
				EntityAlpha(room\LightSpark[i], 1.0)
				PositionEntity(room\LightSpark[i], EntityX(room\LightSpritesPivot[i], True), EntityY(room\LightSpritesPivot[i], True) + 0.05, EntityZ(room\LightSpritesPivot[i], True), True)
				EntityParent(room\LightSpark[i], room\LightSpritesPivot[i])
			EndIf
		EndIf
	Next
End Function

Function CalculateRoomTemplateExtents(r.RoomTemplates)
	If r\DisableOverlapCheck Then Return
	
	GetMeshExtents(GetChild(r\OBJ, 2))
	r\MinX = Mesh_MinX
	r\MinY = Mesh_MinY
	r\MinZ = Mesh_MinZ
	r\MaxX = Mesh_MaxX
	r\MaxY = Mesh_MaxY
	r\MaxZ = Mesh_MaxZ
End Function

Function CalculateRoomExtents(r.Rooms)
	If r\RoomTemplate\DisableOverlapCheck Then Return
	
	; ~ Shrink the extents slightly - we don't care if the overlap is smaller than the thickness of the walls
	Local ShrinkAmount# = 0.05
	
	; ~ Convert from the rooms local space to world space
	TFormVector(r\RoomTemplate\MinX, r\RoomTemplate\MinY, r\RoomTemplate\MinZ, r\OBJ, 0)
	r\MinX = TFormedX() + ShrinkAmount + r\x
	r\MinY = TFormedY() + ShrinkAmount
	r\MinZ = TFormedZ() + ShrinkAmount + r\z
	
	; ~ Convert from the rooms local space to world space
	TFormVector(r\RoomTemplate\MaxX, r\RoomTemplate\MaxY, r\RoomTemplate\MaxZ, r\OBJ, 0)
	r\MaxX = TFormedX() - ShrinkAmount + r\x
	r\MaxY = TFormedY() - ShrinkAmount
	r\MaxZ = TFormedZ() - ShrinkAmount + r\z
	
	If (r\MinX > r\MaxX) Then
		Local TempX# = r\MaxX
		
		r\MaxX = r\MinX
		r\MinX = TempX
	EndIf
	If (r\MinZ > r\MaxZ) Then
		Local TempZ# = r\MaxZ
		
		r\MaxZ = r\MinZ
		r\MinZ = TempZ
	EndIf
End Function

Function CheckRoomOverlap(r1.Rooms, r2.Rooms)
	If (r1\MaxX	=< r2\MinX Or r1\MaxY =< r2\MinY Or r1\MaxZ =< r2\MinZ) Then Return(False)
	If (r1\MinX	>= r2\MaxX Or r1\MinY >= r2\MaxY Or r1\MinZ >= r2\MaxZ) Then Return(False)
	
	Return(True)
End Function

Function PreventRoomOverlap(r.Rooms)
	If r\RoomTemplate\DisableOverlapCheck Then Return
	
	Local r2.Rooms, r3.Rooms
	Local IsIntersecting% = False
	
	; ~ Just skip it when it would try to check for the checkpoints
	If r\RoomTemplate\Name = "checkpoint1" Or r\RoomTemplate\Name = "checkpoint2" Or r\RoomTemplate\Name = "start" Then Return True
	
	; ~ First, check if the room is actually intersecting at all
	For r2 = Each Rooms
		If r2 <> r And (Not r2\RoomTemplate\DisableOverlapCheck) Then
			If CheckRoomOverlap(r, r2) Then
				IsIntersecting = True
				Exit
			EndIf
		EndIf
	Next
	
	; ~ If not, then simply return it as True
	If (Not IsIntersecting)
		Return True
	EndIf
	
	; ~ Room is interseting: First, check if the given room is a ROOM2, so we could potentially just turn it by 180 degrees
	IsIntersecting = False
	
	Local x% = r\x / 8.0
	Local y% = r\z / 8.0
	
	If r\RoomTemplate\Shape = ROOM2 Then
		; ~ Room is a ROOM2, let's check if turning it 180 degrees fixes the overlapping issue
		r\Angle = r\Angle + 180
		RotateEntity(r\OBJ, 0.0, r\Angle, 0.0)
		CalculateRoomExtents(r)
		
		For r2 = Each Rooms
			If r2 <> r And (Not r2\RoomTemplate\DisableOverlapCheck) Then
				If CheckRoomOverlap(r, r2) Then
					; ~ If didn't work then rotate the room back and move to the next step
					IsIntersecting = True
					r\Angle = r\Angle - 180
					RotateEntity(r\OBJ, 0.0, r\Angle, 0.0)
					CalculateRoomExtents(r)
					Exit
				EndIf
			EndIf
		Next
	Else
		IsIntersecting = True
	EndIf
	
	; ~ Room is ROOM2 and was able to be turned by 180 degrees
	If (Not IsIntersecting)
		Return(True)
	EndIf
	
	; ~ Room is either not a ROOM2 or the ROOM2 is still intersecting, now trying to swap the room with another of the same type
	IsIntersecting = True
	
	Local x2%, y2%, Rot%, Rot2%
	
	For r2 = Each Rooms
		If r2 <> r And (Not r2\RoomTemplate\DisableOverlapCheck)  Then
			If r\RoomTemplate\Shape = r2\RoomTemplate\Shape And r\Zone = r2\Zone And (r2\RoomTemplate\Name <> "checkpoint1" And r2\RoomTemplate\Name <> "checkpoint2" And r2\RoomTemplate\Name <> "start") Then
				x = r\x / 8.0
				y = r\z / 8.0
				Rot = r\Angle
				
				x2 = r2\x / 8.0
				y2 = r2\z / 8.0
				Rot2 = r2\Angle
				
				IsIntersecting = False
				
				r\x = x2 * 8.0
				r\z = y2 * 8.0
				r\Angle = Rot2
				PositionEntity(r\OBJ, r\x, r\y, r\z)
				RotateEntity(r\OBJ, 0.0, r\Angle, 0.0)
				CalculateRoomExtents(r)
				
				r2\x = x * 8.0
				r2\z = y * 8.0
				r2\Angle = Rot
				PositionEntity(r2\OBJ, r2\x, r2\y, r2\z)
				RotateEntity(r2\OBJ, 0.0, r2\Angle, 0.0)
				CalculateRoomExtents(r2)
				
				; ~ Make sure neither room overlaps with anything after the swap
				For r3 = Each Rooms
					If (Not r3\RoomTemplate\DisableOverlapCheck) Then
						If r3 <> r Then
							If CheckRoomOverlap(r, r3) Then
								IsIntersecting = True
								Exit
							EndIf
						EndIf
						If r3 <> r2 Then
							If CheckRoomOverlap(r2, r3) Then
								IsIntersecting = True
								Exit
							EndIf
						EndIf	
					EndIf
				Next
				
				; ~ Either the original room or the "reposition" room is intersecting, reset the position of each room to their original one
				If IsIntersecting Then
					r\x = x * 8.0
					r\z = y * 8.0
					r\Angle = Rot
					PositionEntity(r\OBJ, r\x, r\y, r\z)
					RotateEntity(r\OBJ, 0.0, r\Angle, 0.0)
					CalculateRoomExtents(r)
					
					r2\x = x2 * 8.0
					r2\z = y2 * 8.0
					r2\Angle = Rot2
					PositionEntity(r2\OBJ, r2\x, r2\y, r2\z)
					RotateEntity(r2\OBJ, 0.0, r2\Angle, 0.0)
					CalculateRoomExtents(r2)
					
					IsIntersecting = False
				EndIf
			EndIf
		EndIf
	Next
	
	; ~ Room was able to the placed in a different spot
	If (Not IsIntersecting)
		Return(True)
	EndIf
	
	Return(False)
End Function

;~IDEal Editor Parameters:
;~B#11AD
;~C#Blitz3D