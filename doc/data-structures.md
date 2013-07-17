Data Structures
===============

### Speech Recognition Result File
File in [Clojure EDN syntax](http://edn-format.org/) with file extension `.srr`.
The content is a vector of speech recognition results.
#### Example
	[ { :no 0
	    :start 0.3
	    :duration 2.712
	    :confidence 0.5651
	    :text "Hello and welcome"
	    :words [ { :no 0 :confidence 0.9544 :text "Hello" :lexical-form "hello" :pronunciation "həˈləʊ̯" }
	             { :no 1 :confidence 0.8234 :text "and" :lexical-form "and" :pronunciation "ænd" }
	             { :no 2 :confidence 0.8602 :text "welcome" :lexical-form "welcome" :pronunciation "ˈwɛl.kəm" } ]
	    :alternates [ { :no 0
	                    :confidence 0.3521
	                    :text "Hello and elcom"
	                    :words [ ... ] }
	                  ... ] }
	  ... ]
### Speech Recognition Result
A speech recognition result describes the result yielded by the speech
recognition engine, analyzing a section of an audio stream. 
The analyzed section is typically selected by an algorithm which considers
among others values like length of silence, background noises, and
maximal length of a section.
A speech recognition of an audio section yields a number of alternative 
phrases. The phrase with the highest confidence is typically used as the
recognized phrase for the audio section.
#### Slots
* **no**  
  _integer number_ `[0..n]` identifying the result in the context of a video
* **start**  
  _floating point number_ with the begin of the audio section in seconds
* **duration**  
  _floating point number_ with the duration of the audio section in seconds
* **confidence**  
  _floating point number_ `[0..1]` describing the overall confidence of the
  _recognized phrase_ for the audio section
* **text**  
  _string_ with the text of the words in the _recognized phrase_
* **words**  
  _vector_ of the _recognized words_ in the _recognized phrase_
* **alternates** (optional)  
  _vector_ with _alternate phrases_
#### Example
	{ :no 0
	  :start 24.35
	  :duration 4.267
	  :confidence 0.7885
	  :text "a brown fox jumped over the messy hill."
	  :words [ ... ]
	  :alternates [ ... ] }

### Alternate Phrase
An alternate sequence of recognized words for an audio section.
#### Slots
* **no**  
  _integer number_ `[0..n]` identifying the phrase in the context of a _speech 
  recognition result_
* **confidence**  
  _floating point number_ `[0..1]` describing the overall confidence of this phrase
* **text**  
  _string_ with the text of the words in this phrase
* **words**  
  _vector_ of the _recognized words_ in this phrase
#### Example
	{ :no 4
	  :confidence 0.48992
	  :text "a brown fox run about the messy mill."
	  :words [ ... ] }

### Recognized Word
A recognized word is a word in the context of a recognition result. Every word
is recognized with a certain confidence. The confidence values of the words
in a phrase can be combined to an overall confidence for a phrase.
#### Slots
* **no**  
  _integer number_ `[0..n]` identifying the word in a phrase
* **confidence**  
  _floating point number_ `[0..1]` describing the confidence for the recognition
  of this word
* **text**  
  _string_ the text representation of the word in the context of the phrase
* **lexical-form**  
  _string_ the lexical form of the word
* **pronunciation**  
  _string_ the pronunciation of the word in the context of the phrase
#### Example
	{ :no 2
	  :confidence 0.6443
	  :text "brown"
	  :lexical-form "brown"
	  :pronunciation "braʊn" }

