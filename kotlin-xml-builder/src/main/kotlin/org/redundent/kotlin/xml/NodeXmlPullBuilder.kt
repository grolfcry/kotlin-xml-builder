package org.redundent.kotlin.xml

/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

import java.io.IOException
import java.io.Reader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.w3c.dom.DOMException

//protected XmlPullParser pp;
//protected XmlPullParserFactory factory;

class NodeXmlPullBuilder {
    @Throws(XmlPullParserException::class)
    protected fun newParser(): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        return factory.newPullParser()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(reader: Reader): Node {
        val pp = newParser()
        pp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        pp.setInput(reader)
        pp.next()
        return parseSubTree(pp)
    }



    @Throws(XmlPullParserException::class, IOException::class)
    fun parseSubTree(pp: XmlPullParser): Node {
        val process = BuildProcess()
        return process.parseSubTree(pp)
    }

    internal class BuildProcess () {
        private var pp: XmlPullParser? = null
        //private var docFactory: Document? = null
        private var scanNamespaces = true
        private var root: Node? = null
        @Throws(XmlPullParserException::class, IOException::class)
        fun parseSubTree(pp: XmlPullParser): Node {
            this.pp = pp
            return parseSubTree(null)
        }
        @Throws(XmlPullParserException::class, IOException::class)
        fun parseSubTree(_node: Node?): Node {
            var node=_node
            pp!!.require(XmlPullParser.START_TAG, null, null)
            val name = pp!!.name
            val ns = pp!!.namespace
            val prefix = pp!!.prefix
            val qname = if (prefix != null) "$prefix:$name" else name
            if (this.root==null) {
                this.root = xml(qname) {
                    xmlns = ns
                }
                node=this.root
            }
            val parent = Node(qname)
          //  parent.xmlns=ns


            //declare namespaces - quite painful and easy to fail process in DOM2
            declareNamespaces(pp as XmlPullParser, node)

            // process attributes
            for (i in 0 until pp!!.attributeCount) {
                val attrNs = pp!!.getAttributeNamespace(i)
                val attrName = pp!!.getAttributeName(i)
                val attrValue = pp!!.getAttributeValue(i)
                node?.attribute(attrName, attrValue)
//                if (attrNs == null || attrNs.length == 0) {
//                    parent?.attribute(attrName, attrValue)
//                }
//                else {
//                    val attrPrefix = pp!!.getAttributePrefix(i)
//                    val attrQname = if (attrPrefix != null) "$attrPrefix:$attrName" else attrName
//                    parent?. setAttributeNS(attrNs, attrQname, attrValue)
//                }
            }

            // process children
            while (pp!!.next() != XmlPullParser.END_TAG) {
                if (pp!!.eventType == XmlPullParser.START_TAG) {
                    val el = parseSubTree( root!!)
                    parent?.addNode(el)
                }
                else if (pp!!.eventType == XmlPullParser.TEXT) {
                    val text = pp!!.text
                    parent?.text(text.trim { it.isWhitespace() || it == '\r' || it == '\n' })
                } else {
                    throw XmlPullParserException(
                            "unexpected event " + XmlPullParser.TYPES[pp!!.eventType], pp, null)
                }
            }
            pp!!.require(XmlPullParser.END_TAG, ns, name)
            return parent!!
        }

        @Throws(DOMException::class, XmlPullParserException::class)
        private fun declareNamespaces(pp: XmlPullParser, parent: Node?) {
            if (scanNamespaces) {
                scanNamespaces = false
                val top = pp.getNamespaceCount(pp.depth) - 1
                // this loop computes list of all in-scope prefixes
                LOOP@ for (i in top downTo pp.getNamespaceCount(0)) {
                    // make sure that no prefix is duplicated
                    val prefix = pp.getNamespacePrefix(i)
                    for (j in top downTo i + 1) {
                        val prefixJ = pp.getNamespacePrefix(j)
                        if (prefix != null && prefix == prefixJ || prefix != null && prefix === prefixJ) {
                            // prefix is already declared -- skip it
                            continue@LOOP
                        }
                    }
                    declareOneNamespace(pp, i, parent)
                }
            } else {
                for (i in pp.getNamespaceCount(pp.depth - 1) until pp.getNamespaceCount(pp.depth)) {
                    declareOneNamespace(pp, i, parent)
                }
            }
        }

        @Throws(DOMException::class, XmlPullParserException::class)
        private fun declareOneNamespace(pp: XmlPullParser, i: Int, parent: Node?) {
            val xmlnsPrefix = pp.getNamespacePrefix(i)
            val xmlnsUri = pp.getNamespaceUri(i)
            val xmlnsDecl = if (xmlnsPrefix != null) "xmlns:$xmlnsPrefix" else "xmlns"
            parent?.namespace( xmlnsDecl, xmlnsUri) //"http://www.w3.org/2000/xmlns/"
        }


    }


}

