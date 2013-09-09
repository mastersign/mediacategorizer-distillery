Distillery Site Structure
=========================

## Index
Path: `/index.html`
### Context
* Multiple Categories
* Multiple Videos
### Views
* Project
	* Overview
		* Description
		* Statistics
		* Word Hitlist -> Project:Word
	* Cloud
		* Cloud Word -> Project:Word 
	* Matrix
		* Row -> Video:Overview
		* Column -> Category:Overview
		* Cell -> Category:Match 
	* Glossary
* Categories
	* Overview 
		* List -> Category:Overview
* Videos
	* Overview	
		* List -> Video:Overview
* Word*  
  Path: `/words/<word>.inc.html`
	* Statistics
	* Videos (subset) -> Video:Word
	* Categories (subset) -> Category:Word

## Category*
Path: `/categories/<category>/index.html`
### Context
* Single Category
* Multiple Videos (subset)
### Views
* Category (Overview)
	* Description
	* Statistics
	* Word Hitlist -> Category:Word
* Cloud
	* Cloud Word -> Category:Word
* Videos
	* List -> Category:Match
* Glossary
	* List -> Category:Word
* Word*  
  Path: `/categories/<category>/words/<word>.inc.html`
	* Statistics
	* Videos (subset) -> Video:Word
* Match*  
  Path: `/categories/<category>/matches/<video>.inc.html`
	* Match Score
	* Word List -> Category:Word

## Video*
Path: `/videos/<video>/index.html`  
Video-Path: `/video/<video>/<video>.mp4`
### Context
* Single Video
* Multiple Categories (subset)
### Views
* Video
	* Description
	* Statistics
	* Word Hitlist
* Cloud
	* Cloud Word -> Video:Word 
* Transcript
	* Phrase Time -> Playback Position
	* Phrase Word -> Video:Word
* Categories
	* List Item -> Category:Match
* Glossary
	* List Item -> Video:Word
* Word*  
  Path: `/videos/<video>/words/<word>.inc.html`
	* Statistics
	* Occurrences -> Playback Position
	* Categories -> Category:Word

